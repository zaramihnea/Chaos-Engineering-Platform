import pytest

from common import env  # noqa: F401
from common.db import db_available, get_conn
from microservices.catalog.app import CatalogApp


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _db_fetch_products():
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id, name, price FROM products ORDER BY id;")
            rows = cur.fetchall()
    return [{"id": r[0], "name": r[1], "price": float(r[2])} for r in rows]


def _client():
    app = CatalogApp()
    app.setup_routes()
    return app.app.test_client()


def test_http_get_items_matches_db():
    client = _client()
    rv = client.get("/items")
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)
    assert data == _db_fetch_products()


def test_http_get_item_by_id_and_404():
    client = _client()
    rv = client.get("/items/1")
    assert rv.status_code == 200
    data = rv.get_json()
    assert data["id"] == 1

    rv2 = client.get("/items/999999")
    assert rv2.status_code == 404
    err = rv2.get_json()
    assert err.get("error") is not None
