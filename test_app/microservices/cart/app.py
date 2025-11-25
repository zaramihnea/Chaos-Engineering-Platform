from flask import Flask, jsonify, request
from microservices.cart.cart_service import CartService
from aop.logging_aspect import LoggingAspect
from aop.health_info_aspect import HealthInfoAspect
from aop.metrics_aspect import MetricsAspect

class CartApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = CartService("cart", 5002)
        self.aspect = LoggingAspect()
        self.aspect.apply_to_public_methods(
            self.service,
            include=["get_cart", "add_to_cart", "checkout"],
        )
        # Metrics aspect: record latency and request counts
        self.metrics_aspect = MetricsAspect()
        self.metrics_aspect.apply_to_public_methods(
            self.service,
            include=["get_cart", "add_to_cart", "checkout"],
        )
        # Health/Info aspect
        self.health = HealthInfoAspect(self.service, version="1.0.0")
        self.health.apply_to_public_methods(self.service, include=["get_cart", "add_to_cart", "checkout"])

    def setup_routes(self):
        @self.app.route("/cart/<string:user_id>", methods=["GET"])
        def get_cart(user_id):
            cart = self.service.get_cart(user_id)
            return jsonify(cart), 200

        @self.app.route("/cart/<string:user_id>/add", methods=["POST"])
        def add_to_cart(user_id):
            data = request.get_json(force=True) or {}
            product_id = data.get("product_id")
            if not isinstance(product_id, int):
                return jsonify({"error": "product_id is required and must be int"}), 400
            try:
                self.service.add_to_cart(user_id, product_id)
                return jsonify({"ok": True}), 200
            except KeyError as e:
                return jsonify({"error": str(e)}), 400

        @self.app.route("/cart/<string:user_id>/checkout", methods=["POST"])
        def checkout(user_id):
            try:
                result = self.service.checkout(user_id)
                return jsonify(result), 201
            except ValueError as e:
                return jsonify({"error": str(e)}), 400

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            body, content_type = self.service.export_metrics()
            return body, 200, {"Content-Type": content_type}

        @self.app.route("/health", methods=["GET"])
        def health_check():
            return jsonify(self.health.get_health()), 200

        @self.app.route("/info", methods=["GET"])
        def info():
            return jsonify(self.health.get_info()), 200

    def start(self):
        self.app.run(host="0.0.0.0", port=5002)


if __name__ == "__main__":
    cart_app = CartApp()
    cart_app.setup_routes()
    cart_app.start()
