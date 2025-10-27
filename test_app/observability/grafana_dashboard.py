class GrafanaDashboard:
    def __init__(self, dashboard_name: str):
        self.dashboard_name = dashboard_name
        self.panels = []

    def add_panel(self, panel_name: str, metric_query: str):
        pass

    def generate_dashboard_json(self):
        pass
