from microservices.cart.cart_service import CartService
from common.db import get_conn, db_available
import pytest


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _cleanup_bob_open_cart():
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = %s AND status='open');",
                ("bob",),
            )
            cur.execute("DELETE FROM carts WHERE user_id = %s AND status='open';", ("bob",))
        conn.commit()


def test_add_to_cart_and_get_cart_for_bob():
    _cleanup_bob_open_cart()
    svc = CartService("cart", 5002)

    svc.add_to_cart("bob", 1)
    cart = svc.get_cart("bob")
    assert cart["user_id"] == "bob"
    items = cart["items"]
    assert any(i["product_id"] == 1 and i["quantity"] == 1 for i in items)

    # Add again increments quantity
    svc.add_to_cart("bob", 1)
    cart = svc.get_cart("bob")
    items = cart["items"]
    assert any(i["product_id"] == 1 and i["quantity"] == 2 for i in items)


def test_checkout_creates_order_and_closes_cart_for_bob():
    _cleanup_bob_open_cart()
    svc = CartService("cart", 5002)
    svc.add_to_cart("bob", 1)
    svc.add_to_cart("bob", 2)

    result = svc.checkout("bob")
    assert "order_id" in result and isinstance(result["order_id"], int)
    assert result["status"] == "created"

    # cart should no longer be open
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM carts WHERE user_id=%s AND status='open';", ("bob",))
            open_count = cur.fetchone()[0]
    assert open_count == 0
