from microservices.gateway.gateway_service import GatewayService
from common.db import get_conn, db_available
import pytest


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _cleanup_user(user_id: str):
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = %s AND status='open');",
                (user_id,),
            )
            cur.execute("DELETE FROM carts WHERE user_id = %s AND status='open';", (user_id,))
        conn.commit()


def test_gateway_add_to_cart_and_checkout_pay():
    user = "greg"
    _cleanup_user(user)

    gw = GatewayService("gateway", 5000)

    # add to cart orchestrated via route_request
    resp = gw.route_request("cart", "add", {"user_id": user, "item_id": 1})
    assert resp.get("ok") is True

    # checkout then payment via gateway
    resp = gw.route_request("order", "checkout_and_pay", {"user_id": user})
    assert resp.get("status") == "paid"
    assert isinstance(resp.get("order_id"), int)
