"""Chaos agent supporting multiple fault classes (resource, lifecycle, disk, metrics) or focused hog.

Arguments (CLI):
    --mode           focused (default) or mixed
    --fault          When mode=focused: one of
                     cpu_hog|memory_hog|pause|restart|kill_restart|cpu_quota|mem_limit|disk_fill|metrics_block
                     Default: cpu_hog
    --targets        Comma list; if omitted auto-detect running containers excluding prometheus/grafana
    --duration       Total run time seconds (default 300)
    --interval       Seconds between injections (default 30)
    --json           Emit JSON logs (flag)
    --log-file       Path to write action log (default: fault_agent/chaos.log)
    --probe          Enable simple availability/latency probe before/after each action
    --prom-url       Prometheus base URL for queries (default http://localhost:9090)
    --prom-job       Prometheus job name for discovery (default test_app)
    --hog-mem-mb     When fault=memory_hog, MB to allocate (default 1024)

Actions in mixed mode:
    pause              Pause then unpause container.
    restart            Restart container.
    kill_restart       Kill then start container after short downtime.
    cpu_quota          Reduce CPU quota (cgroup) via docker update.
    mem_limit          Reduce memory limit via docker update.
    cpu_hog            Busy loop inside container for short duration.
    memory_hog         Allocate memory inside container then release.
"""

from __future__ import annotations
import os
import random
import time
import json
import subprocess
from typing import List, Dict, Callable, Any
import argparse
import pathlib
import urllib.request
import urllib.error
import urllib.parse
import re
import http.client
import socket

import sys as _sys
import pathlib as _pl
_ws_root = _pl.Path(__file__).resolve().parents[1]
if str(_ws_root) not in _sys.path:
    _sys.path.insert(0, str(_ws_root))

# Import from same directory (backend container has flat structure)
try:
    from docker_functions import (
        list_containers,
        pause_container,
        unpause_container,
        restart_container,
        kill_container,
    )
except ImportError:
    # Fallback for original structure
    from fault_agent.docker_functions import (
        list_containers,
        pause_container,
        unpause_container,
        restart_container,
        kill_container,
    )

DOCKER_CMD = os.environ.get("DOCKER_CMD", "docker")


MONITORING_EXCLUDES = {
    "testapp_prometheus",
    "testapp_grafana",
    "prometheus",
    "grafana",
    "jaeger",
    "zipkin",
    "loki",
    "tempo",
    "otel-collector",
}
MONITORING_PREFIXES = {"prometheus", "grafana", "otel", "loki", "tempo", "zipkin", "jaeger"}


def is_monitoring_container(name: str) -> bool:
    n = (name or "").lower()
    if n in MONITORING_EXCLUDES:
        return True
    return any(n.startswith(p) for p in MONITORING_PREFIXES)


def is_running_container(name: str) -> bool:
    data = list_containers(all=False)
    for c in data.get("containers", []):
        if c.get("Names") == name:
            return True
    return False


def container_exists(name: str) -> bool:
    data = list_containers(all=True)
    for c in data.get("containers", []):
        if c.get("Names") == name:
            return True
    return False


class Monitor:
    def __init__(self, log_path: pathlib.Path) -> None:
        self.log_path = log_path

    def check_target(self, name: str) -> tuple[bool, str | None]:
        if is_monitoring_container(name):
            return False, "target excluded (monitoring container)"
        if not container_exists(name):
            return False, "target not found"
        if not is_running_container(name):
            return False, "target not running"
        return True, None

    def log_violation(self, action: str, target: str, reason: str) -> None:
        write_log_line(self.log_path, {
            "ts": time.time(),
            "action": action,
            "target": target,
            "violation": reason,
        })


def require_valid_target(monitor: Monitor):
    def _decorator(fn: Callable[[str], Dict[str, Any]]):
        def _wrapped(name: str) -> Dict[str, Any]:
            ok, reason = monitor.check_target(name)
            if not ok:
                monitor.log_violation(getattr(fn, "__name__", "action"), name, reason or "invalid target")
                return {"error": reason or "invalid target"}
            return fn(name)
        return _wrapped
    return _decorator


def _run(cmd: List[str], timeout: int = 30) -> Dict[str, Any]:
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return {
            "cmd": " ".join(cmd),
            "stdout": p.stdout.strip(),
            "stderr": p.stderr.strip(),
            "rc": p.returncode,
            "error": None if p.returncode == 0 else p.stderr.strip() or f"exit {p.returncode}",
        }
    except Exception as e:
        return {"cmd": " ".join(cmd), "stdout": "", "stderr": "", "rc": -1, "error": str(e)}


def _parse_size_to_bytes(size_str: str) -> float | None:
    try:
        s = size_str.strip()
        m = re.match(r"([0-9]*\.?[0-9]+)\s*([KMGTP]?i?B)", s, re.I)
        if not m:
            return None
        val = float(m.group(1))
        unit = m.group(2).upper()
        mult = {
            "B": 1,
            "KB": 1000,
            "KIB": 1024,
            "MB": 1000**2,
            "MIB": 1024**2,
            "GB": 1000**3,
            "GIB": 1024**3,
            "TB": 1000**4,
            "TIB": 1024**4,
        }.get(unit, 1)
        return val * mult
    except Exception:
        return None


def docker_stats_once(name: str) -> Dict[str, Any]:
    fmt = "{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.NetIO}}|{{.BlockIO}}"
    res = _run([DOCKER_CMD, "stats", name, "--no-stream", "--format", fmt])
    if res.get("error") or not res.get("stdout"):
        return {"error": res.get("error") or "no output"}
    try:
        cpu, memusage, memperc, netio, blockio = res["stdout"].split("|")
        mem_used, _, mem_limit = memusage.partition("/")
        cpu_val = float(cpu.strip().strip('%')) if '%' in cpu else float(cpu)
        mem_used_b = _parse_size_to_bytes(mem_used)
        mem_limit_b = _parse_size_to_bytes(mem_limit)
        mem_pct = float(memperc.strip().strip('%')) if '%' in memperc else float(memperc)
        return {
            "cpu_pct": cpu_val,
            "mem_used_bytes": mem_used_b,
            "mem_limit_bytes": mem_limit_b,
            "mem_pct": mem_pct,
            "net_io": netio.strip(),
            "block_io": blockio.strip(),
        }
    except Exception as e:
        return {"error": f"parse_stats: {e}", "raw": res.get("stdout")}


def docker_exec(name: str, exec_cmd: List[str], detach: bool = True, timeout: int = 30) -> Dict[str, Any]:
    args = [DOCKER_CMD, "exec"]
    if detach:
        args.append("-d")
    args.append(name)
    args += exec_cmd
    return _run(args, timeout=timeout)


def update_resources(name: str, cpu_percent: int | None = None, mem_limit_mb: int | None = None) -> Dict[str, Any]:
    args = [DOCKER_CMD, "update"]
    if cpu_percent is not None:
        period = 100_000
        quota = max(1, int(period * cpu_percent / 100))
        args += ["--cpu-period", str(period), "--cpu-quota", str(quota)]
    if mem_limit_mb is not None:
        args += ["--memory", f"{mem_limit_mb}m"]
    args.append(name)
    return _run(args)


def burn_cpu_in_container(name: str, seconds: int = 30) -> Dict[str, Any]:
    py = f"import time,math; t=time.time()+{seconds}; x=0\nwhile time.time()<t: x+=math.sqrt(123456)"
    return docker_exec(name, ["python", "-c", py])


def burn_mem_in_container(name: str, mb: int = 128, seconds: int = 30) -> Dict[str, Any]:
    py = (
        "import time; "
        f"a=[bytearray(1024*1024) for _ in range({mb})]; "
        f"time.sleep({seconds})"
    )
    return docker_exec(name, ["python", "-c", py])


def pause_unpause(name: str) -> Dict[str, Any]:
    p = pause_container(name)
    time.sleep(random.choice([5, 10, 15]))
    u = unpause_container(name)
    return {"pause": p, "unpause": u}


def kill_restart(name: str) -> Dict[str, Any]:
    k = kill_container(name)
    time.sleep(random.choice([5, 10]))
    s = _run([DOCKER_CMD, "start", name])
    return {"kill": k, "start": s}


def pick_targets(configured: List[str]) -> List[str]:
    data = list_containers(all=False)
    names = {c.get("Names") for c in data.get("containers", []) if c.get("Names")}
    if configured:
        base = [n for n in configured if n in names]
    else:
        base = list(names)
    return [n for n in base if not is_monitoring_container(n)]


def log_event(event: Dict[str, Any], json_mode: bool) -> None:
    if json_mode:
        print(json.dumps(event))
    else:
        action = event.get("action")
        target = event.get("target")
        status = event.get("result", {}).get("error") or "ok"
        print(f"[CHAOS] action={action} target={target} status={status}")


class Stats:
    def __init__(self) -> None:
        self.total = 0
        self.errors = 0
        self.by_action: Dict[str, int] = {}
        self.by_target: Dict[str, int] = {}
        self.lat_before: List[float] = []
        self.lat_after: List[float] = []
        self.fail_before = 0
        self.fail_after = 0
        self.prom_series: Dict[str, Dict[str, List[float]]] = {}
        self.up_before_vals: List[float] = []
        self.up_after_vals: List[float] = []
        self.up_missing_before = 0
        self.up_missing_after = 0
        self.docker_cpu_before: List[float] = []
        self.docker_cpu_after: List[float] = []
        self.docker_mem_before: List[float] = []
        self.docker_mem_after: List[float] = []

    def add(self, action: str, target: str, ok: bool) -> None:
        self.total += 1
        if not ok:
            self.errors += 1
        self.by_action[action] = self.by_action.get(action, 0) + 1
        self.by_target[target] = self.by_target.get(target, 0) + 1

    def add_probe(self, before_ms: float | None, after_ms: float | None) -> None:
        if before_ms is None:
            self.fail_before += 1
        else:
            self.lat_before.append(before_ms)
        if after_ms is None:
            self.fail_after += 1
        else:
            self.lat_after.append(after_ms)

    def summary(self) -> Dict[str, Any]:
        avg_before = sum(self.lat_before)/len(self.lat_before) if self.lat_before else None
        avg_after = sum(self.lat_after)/len(self.lat_after) if self.lat_after else None
        pct_errors = (self.errors / self.total * 100) if self.total else 0.0
        prom_summary: Dict[str, Any] = {}
        for alias, series in self.prom_series.items():
            b = series.get("before", [])
            a = series.get("after", [])
            b_avg = sum(b)/len(b) if b else None
            a_avg = sum(a)/len(a) if a else None
            delta = None
            if b_avg is not None and a_avg is not None and b_avg != 0:
                delta = round((a_avg - b_avg) / b_avg * 100.0, 2)
            prom_summary[alias] = {
                "avg_before": round(b_avg, 6) if b_avg is not None else None,
                "avg_after": round(a_avg, 6) if a_avg is not None else None,
                "delta_pct": delta,
            }
        up_b = sum(self.up_before_vals)/len(self.up_before_vals) if self.up_before_vals else None
        up_a = sum(self.up_after_vals)/len(self.up_after_vals) if self.up_after_vals else None
        up_delta = None
        if up_b is not None and up_a is not None and up_b != 0:
            up_delta = round((up_a - up_b) / up_b * 100.0, 2)
        cpu_b = sum(self.docker_cpu_before)/len(self.docker_cpu_before) if self.docker_cpu_before else None
        cpu_a = sum(self.docker_cpu_after)/len(self.docker_cpu_after) if self.docker_cpu_after else None
        cpu_delta = None
        if cpu_b is not None and cpu_a is not None and cpu_b != 0:
            cpu_delta = round((cpu_a - cpu_b) / cpu_b * 100.0, 2)
        mem_b = sum(self.docker_mem_before)/len(self.docker_mem_before) if self.docker_mem_before else None
        mem_a = sum(self.docker_mem_after)/len(self.docker_mem_after) if self.docker_mem_after else None
        mem_delta = None
        if mem_b is not None and mem_a is not None and mem_b != 0:
            mem_delta = round((mem_a - mem_b) / mem_b * 100.0, 2)
        return {
            "total_actions": self.total,
            "errors": self.errors,
            "error_pct": round(pct_errors, 2),
            "by_action": self.by_action,
            "by_target": self.by_target,
            "probe": {
                "avg_latency_ms_before": round(avg_before, 2) if avg_before is not None else None,
                "avg_latency_ms_after": round(avg_after, 2) if avg_after is not None else None,
                "failed_probes_before": self.fail_before,
                "failed_probes_after": self.fail_after,
            },
            "prom": prom_summary,
            "uptime": {
                "avg_before": round(up_b, 6) if up_b is not None else None,
                "avg_after": round(up_a, 6) if up_a is not None else None,
                "delta_pct": up_delta,
                "missing_before": self.up_missing_before,
                "missing_after": self.up_missing_after,
            },
            "docker": {
                "cpu_pct": {
                    "avg_before": round(cpu_b, 3) if cpu_b is not None else None,
                    "avg_after": round(cpu_a, 3) if cpu_a is not None else None,
                    "delta_pct": cpu_delta,
                },
                "mem_used_bytes": {
                    "avg_before": round(mem_b, 1) if mem_b is not None else None,
                    "avg_after": round(mem_a, 1) if mem_a is not None else None,
                    "delta_pct": mem_delta,
                },
            },
        }

    def add_prom(self, metrics_before: Dict[str, float | None] | None, metrics_after: Dict[str, float | None] | None) -> None:
        if not metrics_before and not metrics_after:
            return
        aliases = set((metrics_before or {}).keys()) | set((metrics_after or {}).keys())
        for alias in aliases:
            b = (metrics_before or {}).get(alias)
            a = (metrics_after or {}).get(alias)
            if alias not in self.prom_series:
                self.prom_series[alias] = {"before": [], "after": []}
            if isinstance(b, (int, float)):
                self.prom_series[alias]["before"].append(float(b))
            if isinstance(a, (int, float)):
                self.prom_series[alias]["after"].append(float(a))

    def add_uptime(self, up_before: float | None, up_after: float | None) -> None:
        if up_before is None:
            self.up_missing_before += 1
        else:
            try:
                self.up_before_vals.append(float(up_before))
            except Exception:
                self.up_missing_before += 1
        if up_after is None:
            self.up_missing_after += 1
        else:
            try:
                self.up_after_vals.append(float(up_after))
            except Exception:
                self.up_missing_after += 1
    def add_docker_stats(self, before: Dict[str, Any] | None, after: Dict[str, Any] | None) -> None:
        if before and isinstance(before.get("cpu_pct"), (int, float)):
            self.docker_cpu_before.append(float(before["cpu_pct"]))
        if after and isinstance(after.get("cpu_pct"), (int, float)):
            self.docker_cpu_after.append(float(after["cpu_pct"]))
        if before and isinstance(before.get("mem_used_bytes"), (int, float)):
            self.docker_mem_before.append(float(before["mem_used_bytes"]))
        if after and isinstance(after.get("mem_used_bytes"), (int, float)):
            self.docker_mem_after.append(float(after["mem_used_bytes"]))


def write_log_line(path: pathlib.Path, event: Dict[str, Any]) -> None:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a", encoding="utf-8") as f:
            f.write(json.dumps(event) + "\n")
    except Exception:
        pass


def probe_target(name: str, timeout: int = 3) -> float | None:
    port_map = {
        "testapp_gateway": 5000,
        "testapp_catalog": 5001,
        "testapp_cart": 5002,
        "testapp_payment": 5003,
    }
    port = port_map.get(name)
    if port is None:
        return None
    url = f"http://localhost:{port}/metrics"
    start = time.time()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            if resp.status != 200:
                return None
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, socket.timeout, http.client.RemoteDisconnected, ConnectionError, ConnectionResetError, ConnectionRefusedError):
        return None
    return (time.time() - start) * 1000.0


def prom_instance_label(name: str) -> str | None:
    instance_map = {
        "testapp_gateway": "gateway:5000",
        "testapp_catalog": "catalog:5001",
        "testapp_cart": "cart:5002",
        "testapp_payment": "payment:5003",
    }
    return instance_map.get(name)


def discover_instance_for_target(prom_url: str, job: str, target: str) -> str | None:
    port_map = {
        "testapp_gateway": 5000,
        "testapp_catalog": 5001,
        "testapp_cart": 5002,
        "testapp_payment": 5003,
    }
    port = port_map.get(target)
    if port is None:
        return None
    try:
        res = prom_query(prom_url, f"up{{job=\"{job}\"}}")
        if res.get("status") != "success":
            return None
        for item in res.get("data", {}).get("result", []):
            inst = item.get("metric", {}).get("instance")
            if isinstance(inst, str) and inst.endswith(f":{port}"):
                return inst
    except Exception:
        return None
    return None


def prom_query(prom_url: str, promql: str, timeout: int = 4) -> Dict[str, Any]:
    try:
        url = f"{prom_url.rstrip('/')}/api/v1/query?" + urllib.parse.urlencode({"query": promql})
        with urllib.request.urlopen(url, timeout=timeout) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data
    except Exception as e:
        return {"error": str(e)}


def get_prom_queries_for_target(target: str, instance: str) -> Dict[str, str]:
    q: Dict[str, str] = {}
    q["cpu_seconds_per_s"] = f"sum(rate(process_cpu_seconds_total{{instance=\"{instance}\"}}[1m]))"
    q["rss_bytes"] = f"avg(process_resident_memory_bytes{{instance=\"{instance}\"}})"
    if target == "testapp_cart":
        q["throughput_rps"] = f"sum(rate(cart_checkout_total{{instance=\"{instance}\"}}[5m]))"
        q["amount_p90"] = (
            "histogram_quantile(0.9, sum by (le) (rate("
            f"cart_checkout_amount_bucket{{instance=\"{instance}\"}}[5m])))"
        )
    elif target == "testapp_payment":
        q["throughput_rps"] = f"sum(rate(payment_payments_total{{instance=\"{instance}\"}}[5m]))"
        q["fail_rps"] = f"sum(rate(payment_payments_failed_total{{instance=\"{instance}\"}}[5m]))"
        q["amount_p90"] = (
            "histogram_quantile(0.9, sum by (le) (rate("
            f"payment_payment_amount_bucket{{instance=\"{instance}\"}}[5m])))"
        )
    return q


def eval_prom_queries(prom_url: str, queries: Dict[str, str]) -> Dict[str, float | None]:
    out: Dict[str, float | None] = {}
    for alias, ql in queries.items():
        res = prom_query(prom_url, ql)
        val: float | None = None
        try:
            if res.get("status") == "success":
                r = res.get("data", {}).get("result", [])
                if r:
                    v = r[0].get("value", [None, None])[1]
                    if v is not None:
                        val = float(v)
        except Exception:
            val = None
        out[alias] = val
    return out


def disk_fill(name: str) -> Dict[str, Any]:
    mb = random.choice([50, 75, 100, 150])
    py = (
        "import os; sz=%d; path='/tmp/chaos_bloat.dat'; f=open(path,'wb'); chunk=b'0'*1024*1024;" % mb
        + "\nfor _ in range(sz): f.write(chunk); f.flush(); f.close();"
    )
    create = docker_exec(name, ["python", "-c", py], detach=False)
    cleanup = docker_exec(name, ["sh", "-c", "sleep 10 && rm -f /tmp/chaos_bloat.dat"], detach=True)
    return {"create": create, "cleanup": cleanup, "size_mb": mb}


def metrics_block(name: str) -> Dict[str, Any]:
    port_map = {
        "testapp_gateway": 5000,
        "testapp_catalog": 5001,
        "testapp_cart": 5002,
        "testapp_payment": 5003,
    }
    port = port_map.get(name)
    if port is None:
        return {"error": "unknown port"}
    add = docker_exec(name, ["sh", "-c", f"iptables -I INPUT -p tcp --dport {port} -j DROP || true"], detach=False)
    fallback = None
    if add.get("error"):
        fallback = docker_exec(
            name,
            ["python", "-c", "import prometheus_client; prometheus_client.generate_latest=lambda *a,**k: b''"],
            detach=False,
        )
    remove = docker_exec(name, ["sh", "-c", "sleep 15 && iptables -D INPUT 1 || true"], detach=True)
    return {"iptables_add": add, "fallback_patch": fallback, "iptables_remove": remove}


def build_focused_executor(fault: str, hog_mem_mb: int | None = None, monitor: Monitor | None = None) -> Callable[[str], Dict[str, Any]]:
    mapping: Dict[str, Callable[[str], Dict[str, Any]]] = {
        "cpu_hog": lambda n: burn_cpu_in_container(n, seconds=random.choice([15, 25, 35])),
        "memory_hog": lambda n: burn_mem_in_container(
            n,
            mb=(hog_mem_mb if hog_mem_mb is not None else random.choice([512, 1024, 1536])),
            seconds=random.choice([15, 25, 35]),
        ),
        "pause": pause_unpause,
        "restart": restart_container,
        "kill_restart": kill_restart,
        "cpu_quota": lambda n: update_resources(n, cpu_percent=random.choice([20, 40, 60])),
        "mem_limit": lambda n: update_resources(n, mem_limit_mb=random.choice([128, 256, 384])),
        "disk_fill": disk_fill,
        "metrics_block": metrics_block,
    }
    exec_fn = mapping[fault]
    if monitor is not None:
        exec_fn = require_valid_target(monitor)(exec_fn)
    return exec_fn


def build_mixed_actions(hog_mem_mb: int | None = None, monitor: Monitor | None = None) -> Dict[str, Callable[[str], Dict[str, Any]]]:
    actions: Dict[str, Callable[[str], Dict[str, Any]]] = {
        "pause": pause_unpause,
        "restart": restart_container,
        "kill_restart": kill_restart,
        "cpu_quota": lambda n: update_resources(n, cpu_percent=random.choice([20, 40, 60])),
        "mem_limit": lambda n: update_resources(n, mem_limit_mb=random.choice([128, 256, 384])),
        "cpu_hog": lambda n: burn_cpu_in_container(n, seconds=random.choice([15, 25, 35])),
        "memory_hog": lambda n: burn_mem_in_container(
            n,
            mb=(hog_mem_mb if hog_mem_mb is not None else random.choice([512, 1024, 1536])),
            seconds=random.choice([15, 25, 35]),
        ),
        "disk_fill": disk_fill,
        "metrics_block": metrics_block,
    }
    if monitor is not None:
        actions = {k: require_valid_target(monitor)(v) for k, v in actions.items()}
    return actions


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Chaos agent for Dockerized microservices")
    p.add_argument("--mode", choices=["focused", "mixed"], default="focused")
    p.add_argument(
        "--fault",
        default="cpu_hog",
        choices=[
            "cpu_hog",
            "memory_hog",
            "pause",
            "restart",
            "kill_restart",
            "cpu_quota",
            "mem_limit",
            "disk_fill",
            "metrics_block",
        ],
        help="Focused mode action",
    )
    p.add_argument("--targets", help="Comma-separated container names", default="")
    p.add_argument("--duration", type=int, default=300)
    p.add_argument("--interval", type=int, default=30)
    p.add_argument("--json", action="store_true", help="Emit JSON logs")
    p.add_argument("--log-file", default="fault_agent/chaos.log", help="Path to write action log")
    p.add_argument("--probe", action="store_true", help="Probe target /metrics before and after each action")
    p.add_argument("--prom-url", default="http://localhost:9090", help="Prometheus base URL for queries")
    p.add_argument("--prom-job", default="test_app", help="Prometheus job name for discovery")
    p.add_argument("--hog-mem-mb", type=int, default=1024, help="When using memory_hog, memory MB to allocate")
    return p.parse_args()


def main() -> None:
    args = parse_args()
    mode = args.mode
    fault = args.fault
    configured_targets = [t.strip() for t in (args.targets or "").split(",") if t.strip()]
    duration = int(args.duration)
    interval = int(args.interval)
    json_mode = bool(args.json)
    log_path = pathlib.Path(args.log_file)
    do_probe = bool(args.probe)
    prom_url = str(args.prom_url)
    prom_job = str(args.prom_job)
    hog_mem_mb = int(args.hog_mem_mb)
    refresh_every = 10

    monitor = Monitor(log_path=pathlib.Path(args.log_file))
    executor = build_focused_executor(fault, hog_mem_mb=hog_mem_mb, monitor=monitor) if mode == "focused" else None
    actions = build_mixed_actions(hog_mem_mb=hog_mem_mb, monitor=monitor) if mode == "mixed" else None
    end_time = time.time() + duration
    iteration = 0
    targets = pick_targets(configured_targets)
    if not targets:
        if configured_targets:
            for t in configured_targets:
                ok, reason = monitor.check_target(t)
                if not ok:
                    monitor.log_violation(action=fault if mode == "focused" else "mixed", target=t, reason=reason or "ineligible target")
        all_running = list_containers(all=False).get("containers", [])
        eligible_running = [c.get("Names") for c in all_running if c.get("Names") and not is_monitoring_container(c.get("Names"))]
        summary_reason = "no eligible endpoints available" if not eligible_running else "configured endpoints not eligible"
        write_log_line(log_path, {
            "ts": time.time(),
            "action": fault if mode == "focused" else "mixed",
            "summary": {
                "eligible_running": eligible_running,
                "message": summary_reason,
            }
        })
        print("No eligible target containers found (after exclusions).")
        return
    stats = Stats()
    print(f"Chaos agent: mode={mode} fault={fault if mode=='focused' else 'mixed'} targets={targets} duration={duration}s interval={interval}s")
    while time.time() < end_time:
        if iteration % refresh_every == 0:
            targets = pick_targets(configured_targets)
            if not targets:
                print("No targets remaining; exiting.")
                break
        target = random.choice(targets)
        before_ms = probe_target(target) if do_probe else None
        dock_before = docker_stats_once(target)
        up_before = None
        inst = discover_instance_for_target(prom_url, prom_job, target) or prom_instance_label(target)
        metrics_before = None
        if inst:
            q = prom_query(prom_url, f"up{{instance=\"{inst}\"}}")
            try:
                up_before = float(q.get("data", {}).get("result", [{}])[0].get("value", [None, None])[1])
            except Exception:
                up_before = None
            queries_b = get_prom_queries_for_target(target, inst)
            if queries_b:
                metrics_before = eval_prom_queries(prom_url, queries_b)
        if mode == "focused":
            try:
                result = executor(target)
            except Exception as e:
                result = {"error": str(e)}
            event = {"ts": time.time(), "action": fault, "target": target, "result": result, "up_before": up_before, "prom_before": metrics_before}
            log_event(event, json_mode)
            write_log_line(log_path, event)
        else:
            action_name, action_fn = random.choice(list(actions.items()))
            try:
                result = action_fn(target)
            except Exception as e:
                result = {"error": str(e)}
            event = {"ts": time.time(), "action": action_name, "target": target, "result": result, "up_before": up_before, "prom_before": metrics_before}
            log_event(event, json_mode)
            write_log_line(log_path, event)
        ok = not bool(event["result"].get("error")) if isinstance(event.get("result"), dict) else True
        stats.add(event["action"], target, ok)
        after_ms = probe_target(target) if do_probe else None
        up_after = None
        metrics_after = None
        if inst:
            q2 = prom_query(prom_url, f"up{{instance=\"{inst}\"}}")
            try:
                up_after = float(q2.get("data", {}).get("result", [{}])[0].get("value", [None, None])[1])
            except Exception:
                up_after = None
            queries_a = get_prom_queries_for_target(target, inst)
            if queries_a:
                metrics_after = eval_prom_queries(prom_url, queries_a)
        stats.add_prom(metrics_before, metrics_after)
        stats.add_uptime(up_before, up_after)
        dock_after = docker_stats_once(target)
        event_update = {"ts": time.time(), "action": event["action"], "target": target, "probe_ms": {"before": before_ms, "after": after_ms}, "up": {"before": up_before, "after": up_after}, "prom": {"before": metrics_before, "after": metrics_after}, "docker": {"before": dock_before, "after": dock_after}}
        write_log_line(log_path, event_update)
        if do_probe:
            stats.add_probe(before_ms, after_ms)
        stats.add_docker_stats(dock_before if isinstance(dock_before, dict) else None, dock_after if isinstance(dock_after, dict) else None)
        iteration += 1
        time.sleep(interval)
    summary = stats.summary()
    print("Chaos agent finished.")
    print("Summary:")
    print(json.dumps(summary, indent=2))
    write_log_line(log_path, {"ts": time.time(), "summary": summary})


if __name__ == "__main__":
    main()
