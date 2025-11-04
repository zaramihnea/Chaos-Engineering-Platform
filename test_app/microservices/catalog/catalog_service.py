from microservices.base_service import BaseService
from common import env as _env  # noqa: F401
from common.db import db_available


class CatalogService(BaseService):
    def __init__(self, name: str, port: int):
        super().__init__(name, port)
        self._use_db = db_available()
        self._items = [
            {"id": 1, "name": "Widget", "price": 9.99},
            {"id": 2, "name": "Gadget", "price": 14.99},
            {"id": 3, "name": "Doohickey", "price": 4.99},
        ]

    def list_items(self) -> list:
        """Return all catalog items from DB if available, else fallback."""
        if self._use_db:
            from common.db import get_conn

            with get_conn() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT id, name, price FROM products ORDER BY id;")
                    rows = cur.fetchall()
            return [{"id": r[0], "name": r[1], "price": float(r[2])} for r in rows]
        return list(self._items)

    def get_item(self, item_id: int) -> dict:
        """Return an item by id or raise KeyError if not found."""
        if self._use_db:
            from common.db import get_conn

            with get_conn() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT id, name, price FROM products WHERE id=%s;", (item_id,))
                    row = cur.fetchone()
            if not row:
                raise KeyError(f"Item with id {item_id} not found")
            return {"id": row[0], "name": row[1], "price": float(row[2])}

        for item in self._items:
            if item["id"] == item_id:
                return item
        raise KeyError(f"Item with id {item_id} not found")
