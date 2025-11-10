import functools
import time
import traceback
from typing import Any, Callable, Iterable, Optional


class LoggingAspect:
    def __init__(self, logger: Optional[Callable[[str], None]] = None, timing: bool = True, log_args: bool = True, log_result: bool = True):
        self.logger = logger
        self.timing = timing
        self.log_args = log_args
        self.log_result = log_result

    def _emit(self, message: str) -> None:
        if self.logger:
            try:
                self.logger(message)
                return
            except Exception:
                # fall back to print if custom logger fails
                pass
        print(f"[LOG] {message}")

    def before_method(self, method_name: str, args: Iterable[Any], kwargs: dict) -> None:
        if self.log_args:
            self._emit(f"before {method_name} args={list(args)} kwargs={kwargs}")
        else:
            self._emit(f"before {method_name}")

    def after_method(self, method_name: str, result: Any, elapsed_ms: Optional[float]) -> None:
        timing_part = f" elapsed_ms={elapsed_ms:.2f}" if elapsed_ms is not None else ""
        if self.log_result:
            # avoid logging overly large payloads
            r = result
            try:
                preview = repr(r)
            except Exception:
                preview = f"<unrepr {type(r).__name__}>"
            if preview and len(preview) > 300:
                preview = preview[:297] + "..."
            self._emit(f"after {method_name} result={preview}{timing_part}")
        else:
            self._emit(f"after {method_name}{timing_part}")

    def on_exception(self, method_name: str, exception: Exception) -> None:
        tb = traceback.format_exc()
        self._emit(f"exception in {method_name}: {exception}\n{tb}")

    def apply(self, func: Callable) -> Callable:

        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            self.before_method(getattr(func, "__name__", str(func)), args, kwargs)
            start = time.time() if self.timing else None
            try:
                result = func(*args, **kwargs)
            except Exception as e:  # pragma: no cover - logging path
                self.on_exception(getattr(func, "__name__", str(func)), e)
                raise
            elapsed = (time.time() - start) * 1000 if start is not None else None
            self.after_method(getattr(func, "__name__", str(func)), result, elapsed)
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
                wrapped = self.apply(val)
                setattr(obj, attr, wrapped)