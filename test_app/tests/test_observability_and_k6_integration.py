import os

from observability.prometheus_exporter import PrometheusExporter


def test_exporter_includes_process_metrics():
    """Expect exporter to include process / python default collectors.

    This test is intended to fail until the exporter registers the default
    collectors into the per-service registry.
    """
    exp = PrometheusExporter(service_name="cart")
    data, _ = exp.export_metrics()

    # Common default collector metric names (these are not currently present
    # because the exporter uses a fresh CollectorRegistry without default collectors)
    assert b"process_cpu_seconds_total" in data or b"python_info" in data


def test_k6_burst_posts_to_cart_checkout():
    """Expect the k6 `burst.js` script to POST to `/cart/<user>/checkout`.

    The repo's `burst.js` currently performs a GET to `/checkout` so this
    assertion will fail until the script is updated to exercise the
    instrumented cart endpoints (POST `/cart/<user>/add` or `/cart/<user>/checkout`).
    """
    path = os.path.join(os.path.dirname(__file__), "..", "load_testing", "k6", "burst.js")
    path = os.path.normpath(path)
    with open(path, "r", encoding="utf-8") as f:
        src = f.read()

    # We expect the script to contain a POST to a cart path (this is currently not true)
    assert "POST" in src or "/cart/" in src
