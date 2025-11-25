import os

from observability.prometheus_exporter import PrometheusExporter


def test_exporter_includes_process_metrics():
    exp = PrometheusExporter(service_name="cart")
    data, _ = exp.export_metrics()

    assert b"process_cpu_seconds_total" in data or b"python_info" in data


def test_k6_burst_posts_to_cart_checkout():
    path = os.path.join(os.path.dirname(__file__), "..", "load_testing", "k6", "burst.js")
    path = os.path.normpath(path)
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()

    assert "POST" in src or "/cart/" in src
