

from __future__ import annotations
import json
import os
import subprocess
import time
import multiprocessing as mp
from typing import List, Dict, Optional

DOCKER_CMD = os.environ.get("DOCKER_CMD", "docker")


def _run_docker(args: List[str], timeout: int = 30) -> Dict[str, Optional[str]]:
	cmd = [DOCKER_CMD] + args
	try:
		proc = subprocess.run(
			cmd,
			capture_output=True,
			text=True,
			timeout=timeout,
			check=False,
		)
		return {
			"cmd": " ".join(cmd),
			"stdout": proc.stdout.strip(),
			"stderr": proc.stderr.strip(),
			"rc": proc.returncode,
			"error": None if proc.returncode == 0 else proc.stderr.strip() or f"Non-zero exit {proc.returncode}",
		}
	except FileNotFoundError as e:
		return {"cmd": " ".join(cmd), "stdout": None, "stderr": None, "rc": -1, "error": f"Docker not found: {e}"}
	except subprocess.TimeoutExpired:
		return {"cmd": " ".join(cmd), "stdout": None, "stderr": None, "rc": -1, "error": "Timeout"}
	except Exception as e:
		return {"cmd": " ".join(cmd), "stdout": None, "stderr": None, "rc": -1, "error": f"Unexpected error: {e}"}


def list_containers(all: bool = False) -> Dict[str, object]:
	format_str = "{{json .}}"
	args = ["ps", "--format", format_str]
	if all:
		args.insert(1, "-a")
	result = _run_docker(args)
	containers: List[Dict[str, str]] = []
	if result["stdout"] and not result["error"]:
		for line in result["stdout"].splitlines():
			line = line.strip()
			if not line:
				continue
			try:
				containers.append(json.loads(line))
			except Exception:
				pass
	return {"containers": containers, "error": result["error"], "raw": result}


def kill_container(name_or_id: str) -> Dict[str, object]:
	return _run_docker(["kill", name_or_id])


def stop_container(name_or_id: str, timeout: int = 10) -> Dict[str, object]:
	return _run_docker(["stop", "-t", str(timeout), name_or_id])


def restart_container(name_or_id: str) -> Dict[str, object]:
	return _run_docker(["restart", name_or_id])


def pause_container(name_or_id: str) -> Dict[str, object]:
	return _run_docker(["pause", name_or_id])


def unpause_container(name_or_id: str) -> Dict[str, object]:
	return _run_docker(["unpause", name_or_id])



def cpu_worker(stop_time: float, utilization_hint: float) -> None:
	spin_fraction = max(0.0, min(1.0, utilization_hint))
	sleep_fraction = 1.0 - spin_fraction
	cycle = 0.001
	while time.time() < stop_time:
		end_spin = time.time() + cycle * spin_fraction
		while time.time() < end_spin:
			pass
		if sleep_fraction > 0:
			time.sleep(cycle * sleep_fraction)


def hog_cpu(duration_seconds: int = 10, workers: int = 1, utilization_hint: float = 1.0) -> Dict[str, object]:
	if workers < 1:
		return {"error": "workers must be >= 1"}
	stop_time = time.time() + max(0, duration_seconds)
	processes: List[mp.Process] = []
	for _ in range(workers):
		p = mp.Process(target=cpu_worker, args=(stop_time, utilization_hint))
		p.daemon = True
		p.start()
		processes.append(p)
	for p in processes:
		p.join(timeout=max(0, duration_seconds) + 1)
	return {
		"workers": workers,
		"duration": duration_seconds,
		"utilization_hint": utilization_hint,
		"error": None,
	}
