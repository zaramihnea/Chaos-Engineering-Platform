import functools
import os
import platform
import time
from typing import Any, Callable, Optional

from common.db import db_available


class HealthInfoAspect:
    def __init__(self, service: Any, version: str = "1.0.0"):
        self.service = service
        self.version = version
        self.start_time = time.time()
        self.healthy = True
        self.last_error: Optional[str] = None
        self.last_error_ts: Optional[float] = None
        self.last_success_ts: Optional[float] = None
        self.error_count = 0
        self.call_count = 0

    def _on_success(self) -> None:
        self.healthy = True
        self.last_success_ts = time.time()

    def _on_exception(self, exc: Exception) -> None:
        self.healthy = False
        self.last_error = str(exc)
        self.last_error_ts = time.time()
        self.error_count += 1

    def apply(self, func: Callable) -> Callable:
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            self.call_count += 1
            try:
                result = func(*args, **kwargs)
            except Exception as e:
                self._on_exception(e)
                raise
            self._on_success()
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
            val = getattr(obj, attr, None)
            if callable(val):
                setattr(obj, attr, self.apply(val))

    def _uptime_sec(self) -> int:
        return int(time.time() - self.start_time)

    def get_health(self) -> dict:
        return {
            "status": "ok" if self.healthy else "degraded",
            "service": getattr(self.service, "name", None),
            "port": getattr(self.service, "port", None),
            "uptime_sec": self._uptime_sec(),
            "calls": self.call_count,
            "errors": self.error_count,
            "db_available": bool(db_available()),
            "last_error": self.last_error,
            "last_error_ts": self.last_error_ts,
            "last_success_ts": self.last_success_ts,
        }

    def get_info(self) -> dict:
        return {
            "service": getattr(self.service, "name", None),
            "port": getattr(self.service, "port", None),
            "version": self.version,
            "uptime_sec": self._uptime_sec(),
            "python": platform.python_version(),
            "pid": os.getpid(),
            "db_available": bool(db_available()),
        }
