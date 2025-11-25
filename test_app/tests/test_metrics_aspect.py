import time

from observability.prometheus_exporter import PrometheusExporter
from aop.metrics_aspect import MetricsAspect


class DummyService:
    def __init__(self):
        self.name = "cart"
        # exporter should support label_names
        self.metrics = PrometheusExporter(service_name=self.name)

    def instrumented_method(self):
        # simple method that should be wrapped by MetricsAspect
        return "ok"


def test_metrics_aspect_records_request_and_latency():
    svc = DummyService()
    aspect = MetricsAspect()

    # apply to the single public method
    aspect.apply_to_public_methods(svc, include=["instrumented_method"])

    # call the method a couple of times
    for _ in range(2):
        res = svc.instrumented_method()
        assert res == "ok"
        # small pause to ensure elapsed > 0 for histogram observation
        time.sleep(0.001)

    # export metrics and assert the aspect recorded counters and latency
    data, _ = svc.metrics.export_metrics()

    assert b"cart_requests_total" in data
    # method label should appear in the exported metrics
    assert b'method="instrumented_method"' in data
    # histogram parts should be present
    assert b"cart_request_latency_seconds_bucket" in data
    assert b"cart_request_latency_seconds_count" in data
