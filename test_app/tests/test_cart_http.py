import pytest

from common import env  # noqa: F401
from common.db import db_available, get_conn
from microservices.cart.app import CartApp


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
    app = CartApp()
    app.setup_routes()
    return app.app.test_client()


def test_http_add_get_checkout_bob():
    user = "bob"
    _cleanup_user(user)

    client = _client()

    # add item
    rv = client.post(f"/cart/{user}/add", json={"product_id": 1})
    assert rv.status_code == 200
    assert rv.get_json().get("ok") is True

    # add again increments
    rv = client.post(f"/cart/{user}/add", json={"product_id": 1})
    assert rv.status_code == 200

    # get cart
    rv = client.get(f"/cart/{user}")
    assert rv.status_code == 200
    cart = rv.get_json()
    assert cart["user_id"] == user
    assert any(i["product_id"] == 1 and i["quantity"] == 2 for i in cart["items"])

    # checkout
    rv = client.post(f"/cart/{user}/checkout")
    assert rv.status_code == 201
    payload = rv.get_json()
    assert isinstance(payload.get("order_id"), int)
    assert payload.get("status") == "created"
