import pytest

from common import env
from common.db import db_available
from microservices.payment.app import PaymentApp
from microservices.cart.cart_service import CartService


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _client():
    app = PaymentApp()
    app.setup_routes()
    return app.app.test_client()


def test_http_payment_process_marks_paid():
    user = "pam"
    cart = CartService("cart", 5002)
    cart.add_to_cart(user, 1)
    checkout = cart.checkout(user)

    amount = checkout["total_amount"]

    client = _client()
    rv = client.post("/payment/process", json={"user_id": user, "amount": amount})
    assert rv.status_code == 201
    assert rv.get_json().get("paid") is True
