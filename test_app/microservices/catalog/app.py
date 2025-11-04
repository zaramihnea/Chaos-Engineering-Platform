from flask import Flask, jsonify, request
from microservices.catalog.catalog_service import CatalogService
from aop.logging_aspect import LoggingAspect

class CatalogApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = CatalogService("catalog", 5001)
        self.aspect = LoggingAspect()

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

    def start(self):
        self.app.run(host="0.0.0.0", port=5001)

if __name__ == "__main__":
    catalog_app = CatalogApp()
    catalog_app.setup_routes()
    catalog_app.start()
