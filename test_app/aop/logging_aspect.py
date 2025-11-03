import functools

class LoggingAspect:
    def before_method(self, method_name: str):
        pass

    def after_method(self, method_name: str):
        pass

    def on_exception(self, method_name: str, exception: Exception):
        pass

    def apply(self, func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            pass