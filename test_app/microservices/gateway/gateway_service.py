from microservices.base_service import BaseService
from microservices.catalog.catalog_service import CatalogService
from microservices.cart.cart_service import CartService
from microservices.payment.payment_service import PaymentService


class GatewayService(BaseService):
    def __init__(self, name: str, port: int):
        super().__init__(name, port)
        self.catalog = CatalogService("catalog", 5001)
        self.cart = CartService("cart", 5002)
        self.payment = PaymentService("payment", 5003)

    def route_request(self, service_name: str, endpoint: str, payload: dict):
        if service_name == "catalog" and endpoint == "list_items":
            return self.catalog.list_items()
        if service_name == "catalog" and endpoint == "get_item":
            return self.catalog.get_item(payload["item_id"])  # may raise KeyError

        if service_name == "cart" and endpoint == "add":
            item_id = payload["item_id"]
            user_id = payload["user_id"]
            # Ensure product exists first
            _ = self.catalog.get_item(item_id)
            return {"ok": True} if self.cart.add_to_cart(user_id, item_id) else {"ok": False}

        if service_name == "order" and endpoint == "checkout":
            user_id = payload["user_id"]
            return self.cart.checkout(user_id)

        if service_name == "order" and endpoint == "checkout_and_pay":
            user_id = payload["user_id"]
            result = self.cart.checkout(user_id)
            self.payment.process_payment(user_id, result["total_amount"])  # mark paid
            result["status"] = "paid"
            return result

        raise ValueError("Unknown route")

    def handle_response(self, response):
        return response
