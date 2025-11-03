from flask import Flask, jsonify, request
from microservices.cart.cart_service import CartService
from aop.logging_aspect import LoggingAspect

class CartApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = CartService("cart", 5002)
        self.aspect = LoggingAspect()

    def setup_routes(self):
        @self.app.route("/cart/<string:user_id>", methods=["GET"])
        def get_cart(user_id):
            pass

        @self.app.route("/cart/<string:user_id>/add", methods=["POST"])
        def add_to_cart(user_id):
            pass

        @self.app.route("/cart/<string:user_id>/checkout", methods=["POST"])
        def checkout(user_id):
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
    cart_app = CartApp()
    cart_app.setup_routes()
    cart_app.start()
