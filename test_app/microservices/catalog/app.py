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
            pass

        @self.app.route("/items/<int:item_id>", methods=["GET"])
        def get_item(item_id):
            pass

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            pass

    def start(self):
        pass

if __name__ == "__main__":
    catalog_app = CatalogApp()
    catalog_app.setup_routes()
    catalog_app.start()
