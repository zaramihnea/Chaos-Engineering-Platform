import time

from observability.prometheus_exporter import PrometheusExporter
from aop.metrics_aspect import MetricsAspect


class DummyService:
    def __init__(self):
        self.name = "cart"
        self.metrics = PrometheusExporter(service_name=self.name)

    def instrumented_method(self):
        return "ok"


def test_metrics_aspect_records_request_and_latency():
    svc = DummyService()
    aspect = MetricsAspect()

    aspect.apply_to_public_methods(svc, include=["instrumented_method"])

    for _ in range(2):
        res = svc.instrumented_method()
        assert res == "ok"
        time.sleep(0.001)

    data, _ = svc.metrics.export_metrics()

    assert b"cart_requests_total" in data
    assert b'method="instrumented_method"' in data
    assert b"cart_request_latency_seconds_bucket" in data
    assert b"cart_request_latency_seconds_count" in data
