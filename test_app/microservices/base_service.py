class BaseService:
    def __init__(self, name: str, port: int):
        self.name = name
        self.port = port
        self.metrics = None
        self.observers = []
        self.logger = None

    def register_observer(self, observer):
        pass

    def notify_observers(self, metric_name: str, value: float):
        pass

    def get_status(self) -> str:
        pass

    def export_metrics(self):
        pass

    def set_logger(self, logger):
        pass
