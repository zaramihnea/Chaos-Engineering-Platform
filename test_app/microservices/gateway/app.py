from flask import Flask, request, jsonify
from microservices.gateway.gateway_service import GatewayService
from aop.logging_aspect import LoggingAspect

class GatewayApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = GatewayService("gateway", 5000)
        self.aspect = LoggingAspect()

    def setup_routes(self):
        @self.app.route("/route", methods=["POST"])
        def route_request():
            pass

        @self.app.route("/response", methods=["POST"])
        def handle_response():
            pass

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            pass

        @self.app.route("/health", methods=["GET"])
        def health_check():
            pass

    def start(self):
        pass


if __name__ == "__main__":
    gateway_app = GatewayApp()
    gateway_app.setup_routes()
    gateway_app.start()
