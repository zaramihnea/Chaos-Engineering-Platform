import pytest

from common import env  # noqa: F401
from common.db import db_available, get_conn
from microservices.gateway.app import GatewayApp


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


def _client():
    app = GatewayApp()
    app.setup_routes()
    return app.app.test_client()


def test_http_gateway_route_checkout_and_pay():
    user = "gary"
    _cleanup_user(user)

    client = _client()

    # add item via gateway route
    rv = client.post("/route", json={
        "service": "cart",
        "endpoint": "add",
        "payload": {"user_id": user, "item_id": 1}
    })
    assert rv.status_code == 200
    assert rv.get_json().get("ok") is True

    # checkout and pay via gateway
    rv2 = client.post("/route", json={
        "service": "order",
        "endpoint": "checkout_and_pay",
        "payload": {"user_id": user}
    })
    assert rv2.status_code == 200
    body = rv2.get_json()
    assert body.get("status") == "paid"
    assert isinstance(body.get("order_id"), int)
