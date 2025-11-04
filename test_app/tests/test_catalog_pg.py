from microservices.catalog.catalog_service import CatalogService
from common.db import db_available, get_conn
import pytest


pytestmark = pytest.mark.skipif(not db_available(), reason="Postgres not available on this machine")


def _db_fetch_products():
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id, name, price FROM products ORDER BY id;")
            rows = cur.fetchall()
    return [{"id": r[0], "name": r[1], "price": float(r[2])} for r in rows]


def test_catalog_list_items_matches_db():
    expected = _db_fetch_products()
    svc = CatalogService("catalog", 5001)
    items = svc.list_items()
    assert items == expected


def test_catalog_get_item_id_1_matches_db():
    expected = next(i for i in _db_fetch_products() if i["id"] == 1)
    svc = CatalogService("catalog", 5001)
    item = svc.get_item(1)
    assert item == expected
