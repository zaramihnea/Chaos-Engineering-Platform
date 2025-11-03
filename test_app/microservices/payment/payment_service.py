from microservices.base_service import BaseService

class PaymentService(BaseService):
    def process_payment(self, user_id: str, amount: float) -> bool:
        pass

    def refund_payment(self, payment_id: str):
        pass
