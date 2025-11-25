from observability.prometheus_exporter import PrometheusExporter


def test_exporter_creates_and_exports_metrics():
    exp = PrometheusExporter(service_name="cart")

    # create counter and histogram with service+method labels
    c = exp.create_counter("requests_total", "test requests", label_names=["service", "method"])
    h = exp.create_histogram("request_latency_seconds", "latency", label_names=["service", "method"])

    # record one sample
    c.labels(service="cart", method="test").inc()
    h.labels(service="cart", method="test").observe(0.123)

    data, content_type = exp.export_metrics()

    assert content_type is not None
    assert b"cart_requests_total" in data
    # histogram emits bucket, _count and _sum
    assert b"cart_request_latency_seconds_bucket" in data
    assert b"cart_request_latency_seconds_count" in data
