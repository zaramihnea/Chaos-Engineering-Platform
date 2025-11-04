from microservices.cart.cart_service import CartService
from microservices.payment.payment_service import PaymentService
from common.db import get_conn, db_available
import pytest


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _cleanup_user(user_id: str):
    with get_conn() as conn:
        with conn.cursor() as cur:
            # delete open carts for user
            cur.execute(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = %s AND status='open');",
                (user_id,),
            )
            cur.execute("DELETE FROM carts WHERE user_id = %s AND status='open';", (user_id,))
        conn.commit()


def test_process_payment_marks_order_paid():
    user = "carol"
    _cleanup_user(user)

    cart = CartService("cart", 5002)
    cart.add_to_cart(user, 1)
    checkout = cart.checkout(user)

    order_id = checkout["order_id"]
    amount = checkout["total_amount"]

    pay = PaymentService("payment", 5003)
    ok = pay.process_payment(user, amount)
    assert ok is True

    # verify order is marked paid via DB
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT status FROM orders WHERE id = %s;", (order_id,))
            status = cur.fetchone()[0]
            assert status == "paid"
            cur.execute("SELECT COUNT(*) FROM payments WHERE order_id = %s AND status='succeeded';", (order_id,))
            count = cur.fetchone()[0]
            assert count >= 1
