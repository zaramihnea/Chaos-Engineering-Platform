import types
import pathlib
import json

import fault_agent.chaos_agent as ca


# Tests: chaos_agent.is_monitoring_container
def test_is_monitoring_container_rules():
    assert ca.is_monitoring_container('prometheus')
    assert ca.is_monitoring_container('grafana-agent')
    assert not ca.is_monitoring_container('testapp_cart')


# Tests: chaos_agent._parse_size_to_bytes
def test_parse_size_to_bytes():
    assert ca._parse_size_to_bytes('1MiB') == 1024**2
    assert round(ca._parse_size_to_bytes('1.5GB')) == round(1.5 * 1000**3)
    assert ca._parse_size_to_bytes('bad') is None


# Tests: chaos_agent.docker_stats_once (parse path)
def test_docker_stats_once_parse(monkeypatch):
    def fake_run(cmd, timeout=30):
        return {'stdout': '12.5%|123MiB / 1GiB|33.3%|1kB / 2kB|3MB / 4MB', 'error': None}
    monkeypatch.setattr(ca, '_run', fake_run)
    out = ca.docker_stats_once('c')
    assert out['cpu_pct'] == 12.5
    assert out['mem_pct'] == 33.3
    assert out['mem_limit_bytes'] == 1024**3


# Tests: chaos_agent.docker_stats_once (error path)
def test_docker_stats_once_error(monkeypatch):
    monkeypatch.setattr(ca, '_run', lambda *a, **k: {'stdout': '', 'error': 'bad'})
    out = ca.docker_stats_once('c')
    assert 'error' in out


# Tests: chaos_agent.update_resources (cpu+mem flags)
def test_update_resources_builds_flags(monkeypatch):
    captured = {}
    def fake_run(args, timeout=30):
        captured['cmd'] = ' '.join(args)
        return {'rc': 0, 'error': None, 'stdout': '', 'stderr': '', 'cmd': captured['cmd']}
    monkeypatch.setattr(ca, '_run', fake_run)
    ca.update_resources('cart', cpu_percent=40, mem_limit_mb=256)
    cmd = captured['cmd']
    assert '--cpu-period 100000' in cmd and '--cpu-quota' in cmd
    assert '--memory 256m' in cmd and cmd.endswith('cart')


# Tests: chaos_agent.burn_cpu_in_container & burn_mem_in_container
def test_burn_cpu_and_mem(monkeypatch):
    calls = []
    monkeypatch.setattr(ca, 'docker_exec', lambda name, exec_cmd, detach=True, timeout=30: calls.append(exec_cmd) or {'rc': 0})
    ca.burn_cpu_in_container('cart', seconds=5)
    ca.burn_mem_in_container('cart', mb=2, seconds=5)
    assert any('python' in c for c in calls)


# Tests: chaos_agent.pause_unpause
def test_pause_unpause(monkeypatch):
    monkeypatch.setattr(ca, 'pause_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, 'unpause_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, 'time', types.SimpleNamespace(sleep=lambda s: None))
    monkeypatch.setattr(ca, 'random', types.SimpleNamespace(choice=lambda xs: xs[0]))
    res = ca.pause_unpause('cart')
    assert 'pause' in res and 'unpause' in res


# Tests: chaos_agent.kill_restart
def test_kill_restart(monkeypatch):
    monkeypatch.setattr(ca, 'kill_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, 'time', types.SimpleNamespace(sleep=lambda s: None))
    monkeypatch.setattr(ca, 'random', types.SimpleNamespace(choice=lambda xs: xs[0]))
    captured = {}
    monkeypatch.setattr(ca, '_run', lambda args, timeout=30: captured.setdefault('cmd', ' '.join(args)) or {'rc': 0, 'error': None})
    res = ca.kill_restart('cart')
    assert 'kill' in res and 'start' in res
    assert captured['cmd'].endswith('cart')


# Tests: chaos_agent.pick_targets (exclusion logic)
def test_pick_targets_excludes_monitoring(monkeypatch):
    monkeypatch.setattr(ca, 'list_containers', lambda all=False: {'containers': [{'Names': 'testapp_cart'}, {'Names': 'prometheus'}]})
    monkeypatch.setattr(ca, 'is_monitoring_container', lambda n: n == 'prometheus')
    assert ca.pick_targets([]) == ['testapp_cart']


# Tests: chaos_agent.Monitor.check_target
def test_monitor_check_target(monkeypatch, tmp_path):
    m = ca.Monitor(log_path=tmp_path / 'chaos.log')
    monkeypatch.setattr(ca, 'is_monitoring_container', lambda n: n == 'grafana')
    monkeypatch.setattr(ca, 'container_exists', lambda n: n in {'ok', 'stopped'})
    monkeypatch.setattr(ca, 'is_running_container', lambda n: n == 'ok')

    ok, reason = m.check_target('grafana')
    assert ok is False and 'monitoring' in reason
    ok, reason = m.check_target('missing')
    assert ok is False and 'not found' in reason
    ok, reason = m.check_target('stopped')
    assert ok is False and 'not running' in reason
    ok, reason = m.check_target('ok')
    assert ok is True and reason is None


# Tests: chaos_agent.require_valid_target (invalid target logs)
def test_require_valid_target_logs(monkeypatch, tmp_path):
    m = ca.Monitor(log_path=tmp_path / 'chaos.log')
    log_calls = []
    monkeypatch.setattr(ca, 'write_log_line', lambda path, ev: log_calls.append(ev))
    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (False, 'bad'))

    def fn(name): return {'ok': True}

    wrapped = ca.require_valid_target(m)(fn)
    res = wrapped('x')
    assert res['error'] == 'bad'
    assert log_calls and log_calls[0]['violation'] == 'bad'


# Tests: chaos_agent.prom_query (success) & eval_prom_queries
def test_prom_query_and_eval(monkeypatch):
    class FakeResponse:
        def __enter__(self):
            return self
        def __exit__(self, exc_type, exc, tb):
            return False
        def read(self):
            return json.dumps({'status': 'success', 'data': {'result': []}}).encode('utf-8')

    class FakeRequestModule:
        @staticmethod
        def urlopen(url, timeout=4):
            return FakeResponse()

    monkeypatch.setattr(ca, 'urllib', types.SimpleNamespace(
        request=FakeRequestModule,
        parse=ca.urllib.parse,
        error=ca.urllib.error
    ))
    data = ca.prom_query('http://localhost:9090', 'up')
    assert data.get('status') == 'success'

    monkeypatch.setattr(ca, 'prom_query', lambda u, q: {'status': 'success', 'data': {'result': [{'value': [0, '1.23']}]} } )
    vals = ca.eval_prom_queries('http://localhost:9090', {'x': 'up'})
    assert vals['x'] == 1.23


# Tests: chaos_agent.disk_fill & metrics_block (iptables fallback)
def test_disk_fill_and_metrics_block(monkeypatch):
    calls = []
    def fake_exec(name, cmd, detach=True, timeout=30):
        calls.append((' '.join(cmd), detach))
        if 'iptables -I' in ' '.join(cmd):
            return {'error': 'iptables unavailable'}
        return {'rc': 0, 'error': None}
    monkeypatch.setattr(ca, 'docker_exec', fake_exec)
    res1 = ca.disk_fill('cart')
    assert 'create' in res1 and 'cleanup' in res1

    res2 = ca.metrics_block('testapp_cart')
    assert 'iptables_add' in res2 and 'iptables_remove' in res2
    assert isinstance(res2.get('fallback_patch'), dict)


# Tests: chaos_agent.log_event (JSON and text)
def test_log_event_json_and_text(monkeypatch, capsys):
    event = {"ts": 123.0, "action": "cpu_hog", "target": "cart", "result": {"ok": True}}
    ca.log_event(event, json_mode=True)
    out = capsys.readouterr().out.strip()
    assert out.startswith('{') and 'cpu_hog' in out

    # Text mode prints a human-readable message
    ca.log_event(event, json_mode=False)
    out2 = capsys.readouterr().out.strip()
    assert 'action=cpu_hog' in out2 and 'target=cart' in out2


# Tests: chaos_agent.write_log_line (happy path)
def test_write_log_line(tmp_path):
    p = tmp_path / 'log.jsonl'
    ev = {"ts": 1.0, "action": "test", "ok": True}
    ca.write_log_line(p, ev)
    content = p.read_text().strip()
    data = json.loads(content)
    assert data["action"] == "test" and data["ok"] is True


# Tests: chaos_agent.discover_instance_for_target
def test_discover_instance_for_target(monkeypatch):
    def fake_query(url, q):
        return {
            "status": "success",
            "data": {
                "result": [
                    {"metric": {"instance": "cart:5002"}},
                    {"metric": {"instance": "payment:5004"}},
                ]
            }
        }
    monkeypatch.setattr(ca, 'prom_query', fake_query)
    inst = ca.discover_instance_for_target('http://localhost:9090', 'test_app', 'testapp_cart')
    assert inst == 'cart:5002'


# Tests: chaos_agent.prom_instance_label
def test_prom_instance_label_mapping():
    assert ca.prom_instance_label('testapp_cart').endswith(':5002')
    assert ca.prom_instance_label('testapp_payment').endswith(':5003')


# Tests: chaos_agent.log_event (text ok/error)
def test_log_event_text_modes(capsys):
    event_ok = {"ts": 123.0, "action": "restart", "target": "catalog", "result": {"ok": True}}
    ca.log_event(event_ok, json_mode=False)
    out_ok = capsys.readouterr().out
    assert 'action=restart' in out_ok and 'target=catalog' in out_ok and 'status=ok' in out_ok

    event_err = {"ts": 124.0, "action": "restart", "target": "catalog", "result": {"error": "boom"}}
    ca.log_event(event_err, json_mode=False)
    out_err = capsys.readouterr().out
    assert 'status=boom' in out_err


# Tests: chaos_agent.update_resources (cpu-only)
def test_update_resources_cpu_only(monkeypatch):
    captured = {}
    monkeypatch.setattr(ca, '_run', lambda args, timeout=30: captured.setdefault('cmd', ' '.join(args)) or {'rc': 0})
    ca.update_resources('cart', cpu_percent=50, mem_limit_mb=None)
    assert '--cpu-quota' in captured['cmd'] and '--memory' not in captured['cmd']


# Tests: chaos_agent.update_resources (mem-only)
def test_update_resources_mem_only(monkeypatch):
    captured = {}
    monkeypatch.setattr(ca, '_run', lambda args, timeout=30: captured.setdefault('cmd', ' '.join(args)) or {'rc': 0})
    ca.update_resources('cart', cpu_percent=None, mem_limit_mb=128)
    assert '--memory 128m' in captured['cmd'] and '--cpu-quota' not in captured['cmd']


# Tests: chaos_agent.is_running_container & container_exists
def test_container_exists_and_is_running(monkeypatch):
    monkeypatch.setattr(ca, 'list_containers', lambda all=False: {
        'containers': [{'Names': 'testapp_cart'}]
    })
    assert ca.is_running_container('testapp_cart') is True
    assert ca.is_running_container('missing') is False

    monkeypatch.setattr(ca, 'list_containers', lambda all=True: {
        'containers': [{'Names': 'testapp_cart'}, {'Names': 'stopped_one'}]
    })
    assert ca.container_exists('testapp_cart') is True
    assert ca.container_exists('stopped_one') is True
    assert ca.container_exists('missing') is False


# Tests: chaos_agent.metrics_block (fallback + remove)
def test_metrics_block_fallback(monkeypatch):
    calls = {}
    def fake_exec(name, cmd, detach=True, timeout=30):
        scmd = ' '.join(cmd)
        if 'iptables -I' in scmd:
            return {'error': 'iptables unavailable'}
        calls.setdefault('cmds', []).append(scmd)
        return {'rc': 0}
    monkeypatch.setattr(ca, 'docker_exec', fake_exec)
    res = ca.metrics_block('testapp_cart')
    assert res['iptables_add'].get('error')
    assert isinstance(res['fallback_patch'], dict)
    assert 'iptables -D' in calls['cmds'][-1]


# Tests: chaos_agent.probe_target (unknown/success)
def test_probe_target_unknown_and_success(monkeypatch):
    assert ca.probe_target('unknown') is None

    class _Resp:
        status = 200
        def __enter__(self): return self
        def __exit__(self, *a): return False
    monkeypatch.setattr(ca.urllib.request, 'urlopen', lambda url, timeout=3: _Resp())
    monkeypatch.setattr(ca, 'time', types.SimpleNamespace(time=lambda: 1.0, sleep=lambda s: None))
    assert isinstance(ca.probe_target('testapp_cart'), float)


# Tests: chaos_agent.build_focused_executor (all fault mappings)
def test_build_focused_executor_all_faults(monkeypatch):
    monkeypatch.setattr(ca, 'docker_exec', lambda *a, **k: {'rc': 0})
    monkeypatch.setattr(ca, 'pause_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, 'unpause_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, 'kill_container', lambda n: {'ok': True})
    monkeypatch.setattr(ca, '_run', lambda args, timeout=30: {'rc': 0})
    monkeypatch.setattr(ca, 'update_resources', lambda n, **k: {'rc': 0})
    monkeypatch.setattr(ca, 'disk_fill', lambda n: {'rc': 0})
    monkeypatch.setattr(ca, 'metrics_block', lambda n: {'rc': 0})
    faults = [
        'cpu_hog','memory_hog','pause','restart','kill_restart','cpu_quota','mem_limit','disk_fill','metrics_block'
    ]
    for f in faults:
        fn = ca.build_focused_executor(f, hog_mem_mb=64, monitor=None)
        out = fn('testapp_cart')
        assert isinstance(out, dict)


# Tests: chaos_agent.prom_query (error path)
def test_prom_query_error_path(monkeypatch):
    def raiser(*a, **k):
        raise RuntimeError('boom')
    monkeypatch.setattr(ca.urllib.request, 'urlopen', raiser)
    data = ca.prom_query('http://localhost:9090', 'up')
    assert 'error' in data


# Tests: chaos_agent.Stats.add_uptime & summary (invalid values)
def test_stats_add_uptime_invalid_values():
    s = ca.Stats()
    s.add_uptime('not-a-number', None)
    s.add_uptime(None, 'nan')
    summary = s.summary()
    assert summary['uptime']['missing_before'] >= 1
    assert summary['uptime']['missing_after'] >= 1


# Tests: chaos_agent.pick_targets (configured filter)
def test_pick_targets_with_configured_filter(monkeypatch):
    monkeypatch.setattr(ca, 'list_containers', lambda all=False: {
        'containers': [
            {'Names': 'testapp_cart'},
            {'Names': 'testapp_payment'},
            {'Names': 'prometheus'},
        ]
    })
    monkeypatch.setattr(ca, 'is_monitoring_container', lambda n: n == 'prometheus')
    filtered = ca.pick_targets(['testapp_cart'])
    assert filtered == ['testapp_cart']


# Tests: chaos_agent.require_valid_target (valid target executes)
def test_require_valid_target_success(monkeypatch, tmp_path):
    m = ca.Monitor(log_path=tmp_path / 'chaos.log')
    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (True, None))
    ran = {}
    def fn(name): ran['ok'] = True; return {'ok': True}
    wrapped = ca.require_valid_target(m)(fn)
    out = wrapped('ok')
    assert ran.get('ok') is True and out['ok'] is True


# Tests: chaos_agent.build_focused_executor/build_mixed_actions with monitor wrapping
def test_build_focused_and_mixed_wrapping(monkeypatch, tmp_path):
    m = ca.Monitor(log_path=tmp_path / 'chaos.log')
    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (False, 'invalid'))
    exec_fn = ca.build_focused_executor('cpu_hog', monitor=m)
    res = exec_fn('any')
    assert 'error' in res

    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (True, None))
    actions = ca.build_mixed_actions(monitor=m)
    for name, fn in actions.items():
        out = fn('ok')
        assert isinstance(out, dict)


# Tests: chaos_agent.Stats.add/add_probe/add_docker_stats & summary
def test_stats_aggregation():
    s = ca.Stats()
    s.add('a', 't', ok=True)
    s.add('b', 't', ok=False)
    s.add_probe(10.0, None)
    s.add_prom({'cpu': 1.0}, {'cpu': 2.0})
    s.add_uptime(1.0, 0.0)
    s.add_docker_stats({'cpu_pct': 10.0, 'mem_used_bytes': 100.0}, {'cpu_pct': 20.0, 'mem_used_bytes': 200.0})
    summary = s.summary()
    assert summary['total_actions'] == 2
    assert summary['errors'] == 1
    assert summary['probe']['avg_latency_ms_before'] == 10.0
    assert summary['prom']['cpu']['avg_after'] == 2.0
    assert summary['docker']['cpu_pct']['avg_before'] == 10.0


# Tests: chaos_agent.Stats.add_prom & summary delta
def test_stats_prom_delta():
    s = ca.Stats()
    s.add_prom({'m': 10.0}, {'m': 15.0})
    summ = s.summary()
    assert summ['prom']['m']['avg_before'] == 10.0
    assert summ['prom']['m']['avg_after'] == 15.0
    assert summ['prom']['m']['delta_pct'] == 50.0


# Tests: chaos_agent.main (focused mode, duration=0)
def test_main_smoke(monkeypatch, tmp_path):
    monkeypatch.setattr(ca, 'parse_args', lambda: types.SimpleNamespace(
        mode='focused', fault='cpu_hog', targets='testapp_cart', duration=0, interval=0,
        json=True, log_file=str(tmp_path / 'chaos.log'), probe=False,
        prom_url='http://localhost:9090', prom_job='test_app', hog_mem_mb=64,
    ))
    monkeypatch.setattr(ca, 'list_containers', lambda all=False: {'containers': [{'Names': 'testapp_cart'}]})
    monkeypatch.setattr(ca, 'is_monitoring_container', lambda n: False)
    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (True, None))
    monkeypatch.setattr(ca, 'docker_exec', lambda name, cmd, detach=True, timeout=30: {'rc': 0})
    monkeypatch.setattr(ca, 'docker_stats_once', lambda name: {'cpu_pct': 1.0, 'mem_used_bytes': 100.0})
    monkeypatch.setattr(ca, 'discover_instance_for_target', lambda u, j, t: None)
    monkeypatch.setattr(ca, 'prom_instance_label', lambda t: None)
    monkeypatch.setattr(ca, 'prom_query', lambda u, q: {'status': 'success', 'data': {'result': []}})
    monkeypatch.setattr(ca, 'eval_prom_queries', lambda u, qs: {})
    monkeypatch.setattr(ca, 'time', types.SimpleNamespace(time=lambda: 0.0, sleep=lambda x: None))
    ca.main()
    assert (tmp_path / 'chaos.log').exists()


# Tests: chaos_agent.main (mixed mode early-exit + summary)
def test_main_mixed_no_targets_summary(monkeypatch, tmp_path):
    monkeypatch.setattr(ca, 'parse_args', lambda: types.SimpleNamespace(
        mode='mixed', fault='cpu_hog', targets='testapp_cart', duration=0, interval=0,
        json=False, log_file=str(tmp_path / 'chaos.log'), probe=False,
        prom_url='http://localhost:9090', prom_job='test_app', hog_mem_mb=64,
    ))
    monkeypatch.setattr(ca, 'list_containers', lambda all=False: {'containers': []})
    monkeypatch.setattr(ca.Monitor, 'check_target', lambda self, n: (False, 'not running'))
    ca.main()
    content = (tmp_path / 'chaos.log').read_text()
    assert 'summary' in content


# Tests: chaos_agent.get_prom_queries_for_target (cart/payment)
def test_get_prom_queries_for_target():
    cart_q = ca.get_prom_queries_for_target('testapp_cart', 'cart:5002')
    assert 'throughput_rps' in cart_q and 'amount_p90' in cart_q
    pay_q = ca.get_prom_queries_for_target('testapp_payment', 'payment:5003')
    assert 'fail_rps' in pay_q and 'amount_p90' in pay_q


# Tests: chaos_agent.probe_target (exception path)
def test_probe_target_error_path(monkeypatch):
    def raiser(*a, **k):
        raise TimeoutError('timeout')
    monkeypatch.setattr(ca.urllib.request, 'urlopen', raiser)
    assert ca.probe_target('testapp_cart') is None


# Tests: chaos_agent.write_log_line (exception path)
def test_write_log_line_error(monkeypatch, tmp_path):
    # Patch Path.open to raise to exercise exception path
    p = tmp_path / 'bad.log'
    def bad_open(self, *a, **k):
        raise OSError('nope')
    monkeypatch.setattr(pathlib.Path, 'open', bad_open)
    # Should not raise
    ca.write_log_line(p, {'ok': True})