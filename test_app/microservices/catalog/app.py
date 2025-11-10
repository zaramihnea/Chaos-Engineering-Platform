from flask import Flask, jsonify, request
from microservices.catalog.catalog_service import CatalogService
from aop.logging_aspect import LoggingAspect
from aop.health_info_aspect import HealthInfoAspect

class CatalogApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = CatalogService("catalog", 5001)
        self.aspect = LoggingAspect()
        self.aspect.apply_to_public_methods(
            self.service,
            include=["list_items", "get_item"],
        )
        self.health = HealthInfoAspect(self.service, version="1.0.0")
        self.health.apply_to_public_methods(self.service, include=["list_items", "get_item"])

    def setup_routes(self):
        @self.app.route("/items", methods=["GET"])
        def get_items():
            items = self.service.list_items()
            return jsonify(items), 200

        @self.app.route("/items/<int:item_id>", methods=["GET"])
        def get_item(item_id):
            try:
                item = self.service.get_item(item_id)
                return jsonify(item), 200
            except KeyError as e:
                return jsonify({"error": str(e)}), 404

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            # Placeholder metrics endpoint for iteration 2
            return "# metrics\n", 200

        @self.app.route("/health", methods=["GET"])
        def health_check():
            return jsonify(self.health.get_health()), 200

        @self.app.route("/info", methods=["GET"])
        def info():
            return jsonify(self.health.get_info()), 200

    def start(self):
        self.app.run(host="0.0.0.0", port=5001)

if __name__ == "__main__":
    catalog_app = CatalogApp()
    catalog_app.setup_routes()
    catalog_app.start()
