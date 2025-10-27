class MetricsObserver:
    def __init__(self, observer_name: str):
        self.observer_name = observer_name

    def update(self, metric_name: str, value: float):
        pass
