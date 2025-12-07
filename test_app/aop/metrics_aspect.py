import functools
import time
from typing import Any, Callable, Iterable, Optional


class MetricsAspect:
    def __init__(self):
        self._cache = {}

    def _ensure_metrics(self, service) -> tuple:
        key = id(service)
        cached = self._cache.get(key)
        if cached:
            return cached

        try:
            req_counter = service.metrics.create_counter(
                "requests_total",
                "Number of method calls",
                label_names=["service", "method"],
            )
            err_counter = service.metrics.create_counter(
                "requests_failed_total",
                "Number of failed method calls",
                label_names=["service", "method"],
            )
            latency_hist = service.metrics.create_histogram(
                "request_latency_seconds",
                "Method call latency (seconds)",
                label_names=["service", "method"],
            )
        except Exception:
            req_counter = err_counter = latency_hist = None
        self._cache[key] = (req_counter, err_counter, latency_hist)
        return self._cache[key]

    def apply(self, func: Callable, service) -> Callable:

        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            req_counter, err_counter, latency_hist = self._ensure_metrics(service)
            method_name = getattr(func, "__name__", str(func))
            start = time.time()
            try:
                result = func(*args, **kwargs)
            except Exception:
                if err_counter is not None:
                    try:
                        err_counter.labels(service=service.name, method=method_name).inc()
                    except Exception as _e:
                        print(f"[metrics_aspect] failed to inc err_counter for {service.name}.{method_name}: {_e}")
                raise
            finally:
                elapsed = time.time() - start
                if req_counter is not None:
                    try:
                        req_counter.labels(service=service.name, method=method_name).inc()
                    except Exception as _e:
                        print(f"[metrics_aspect] failed to inc req_counter for {service.name}.{method_name}: {_e}")
                if latency_hist is not None:
                    try:
                        latency_hist.labels(service=service.name, method=method_name).observe(elapsed)
                    except Exception as _e:
                        print(f"[metrics_aspect] failed to observe latency for {service.name}.{method_name}: {_e}")
            return result

        return wrapper

    def apply_to_public_methods(
        self,
        obj: Any,
        include: Optional[list[str]] = None,
        exclude: Optional[list[str]] = None,
    ) -> None:
        exclude = set(exclude or [])
        for attr in dir(obj):
            if attr.startswith("_"):
                continue
            if include is not None and attr not in include:
                continue
            if attr in exclude:
                continue
            try:
                val = getattr(obj, attr)
            except Exception:
                continue
            if callable(val):
                wrapped = self.apply(val, obj)
                setattr(obj, attr, wrapped)
