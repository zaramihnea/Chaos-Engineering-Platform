from flask import Flask, jsonify, request
from microservices.payment.payment_service import PaymentService
from aop.logging_aspect import LoggingAspect

class PaymentApp:
    def __init__(self):
        self.app = Flask(__name__)
        self.service = PaymentService("payment", 5003)
        self.aspect = LoggingAspect()

    def setup_routes(self):
        @self.app.route("/payment/process", methods=["POST"])
        def process_payment():
            pass

        @self.app.route("/payment/refund", methods=["POST"])
        def refund_payment():
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
    payment_app = PaymentApp()
    payment_app.setup_routes()
    payment_app.start()
