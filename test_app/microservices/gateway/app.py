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
            body = request.get_json(force=True) or {}
            service = body.get("service")
            endpoint = body.get("endpoint")
            payload = body.get("payload", {})
            if not service or not endpoint:
                return jsonify({"error": "service and endpoint are required"}), 400
            try:
                result = self.service.route_request(service, endpoint, payload)
                return jsonify(result), 200
            except Exception as e:
                return jsonify({"error": str(e)}), 400

        @self.app.route("/response", methods=["POST"])
        def handle_response():
            body = request.get_json(force=True) or {}
            return jsonify(self.service.handle_response(body)), 200

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            return "# metrics\n", 200

        @self.app.route("/health", methods=["GET"])
        def health_check():
            return jsonify({"status": "ok"}), 200

    def start(self):
        self.app.run(host="0.0.0.0", port=5000)


if __name__ == "__main__":
    gateway_app = GatewayApp()
    gateway_app.setup_routes()
    gateway_app.start()
