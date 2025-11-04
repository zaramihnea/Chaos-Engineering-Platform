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
            data = request.get_json(force=True) or {}
            user_id = data.get("user_id")
            amount = data.get("amount")
            if not user_id or not isinstance(amount, (int, float)):
                return jsonify({"error": "user_id and amount are required"}), 400
            try:
                ok = self.service.process_payment(user_id, float(amount))
                return jsonify({"paid": bool(ok)}), 201
            except ValueError as e:
                return jsonify({"error": str(e)}), 400

        @self.app.route("/payment/refund", methods=["POST"])
        def refund_payment():
            data = request.get_json(force=True) or {}
            payment_id = data.get("payment_id")
            if not payment_id:
                return jsonify({"error": "payment_id is required"}), 400
            self.service.refund_payment(payment_id)
            return jsonify({"refunded": True}), 200

        @self.app.route("/metrics", methods=["GET"])
        def metrics():
            return "# metrics\n", 200

        @self.app.route("/health", methods=["GET"])
        def health_check():
            return jsonify({"status": "ok"}), 200

    def start(self):
        self.app.run(host="0.0.0.0", port=5003)


if __name__ == "__main__":
    payment_app = PaymentApp()
    payment_app.setup_routes()
    payment_app.start()
