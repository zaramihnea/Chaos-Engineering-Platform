from observability.prometheus_exporter import PrometheusExporter


class BaseService:
    def __init__(self, name: str, port: int):
        self.name = name
        self.port = port
        self.metrics = PrometheusExporter(service_name=name)
        self.observers = []
        self.logger = None

    def register_observer(self, observer):
        self.observers.append(observer)

    def notify_observers(self, metric_name: str, value: float):
        for observer in self.observers:
            try:
                observer.update(metric_name, value)
            except Exception:
                # Ignore observer errors to avoid impacting main flow
                continue

    def get_status(self) -> str:
        return "ok"

    def export_metrics(self):
        return self.metrics.export_metrics()

    def set_logger(self, logger):
        self.logger = logger
