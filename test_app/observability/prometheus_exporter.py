from typing import Dict, Any, List, Optional

from prometheus_client import (
    Counter,
    Histogram,
    CollectorRegistry,
    generate_latest,
    CONTENT_TYPE_LATEST,
)
from prometheus_client import Gauge

try:
    from prometheus_client import core as _prom_core
except Exception:
    _prom_core = None


class PrometheusExporter:
    def __init__(self, service_name: str):
        self.service_name = service_name
        self.registry = CollectorRegistry()
        if _prom_core is not None:
            try:
                self.registry.register(_prom_core.ProcessCollector())
            except Exception:
                pass
            try:
                self.registry.register(_prom_core.PlatformCollector())
            except Exception:
                pass

        try:
            Gauge("python_info", "Python runtime info", registry=self.registry).set(1)
        except Exception:
            pass

        self.metrics: Dict[str, Any] = {}

    def create_counter(self, name: str, description: str, label_names: Optional[List[str]] = None) -> Counter:
        if label_names is None:
            label_names = ["service"]
        full_name = f"{self.service_name}_{name}"
        counter = Counter(full_name, description, label_names, registry=self.registry)
        self.metrics[name] = counter
        return counter

    def create_histogram(self, name: str, description: str, label_names: Optional[List[str]] = None) -> Histogram:
        if label_names is None:
            label_names = ["service"]
        full_name = f"{self.service_name}_{name}"
        histogram = Histogram(full_name, description, label_names, registry=self.registry)
        self.metrics[name] = histogram
        return histogram

    def export_metrics(self):
        data = generate_latest(self.registry)
        return data, CONTENT_TYPE_LATEST
