from microservices.base_service import BaseService
from common import env as _env  # noqa: F401


class CartService(BaseService):
    def __init__(self, name: str, port: int):
        super().__init__(name, port)
        # business metrics
        self.checkout_counter = self.metrics.create_counter(
            "checkout_total", "Number of successful checkouts"
        )
        self.checkout_amount_hist = self.metrics.create_histogram(
            "checkout_amount", "Order amount at checkout"
        )

    def add_to_cart(self, user_id: str, item_id: int):
        """Create or find an open cart for user and add/increment item."""
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                # Ensure product exists
                cur.execute("SELECT id FROM products WHERE id=%s;", (item_id,))
                if not cur.fetchone():
                    raise KeyError(f"Product {item_id} not found")

                # Find or create open cart
                cur.execute("SELECT id FROM carts WHERE user_id=%s AND status='open' ORDER BY id DESC LIMIT 1;", (user_id,))
                row = cur.fetchone()
                if row:
                    cart_id = row[0]
                else:
                    cur.execute("INSERT INTO carts (user_id, status) VALUES (%s, 'open') RETURNING id;", (user_id,))
                    cart_id = cur.fetchone()[0]

                # Upsert cart item quantity
                cur.execute(
                    """
                    INSERT INTO cart_items (cart_id, product_id, quantity)
                    VALUES (%s, %s, 1)
                    ON CONFLICT (cart_id, product_id)
                    DO UPDATE SET quantity = cart_items.quantity + 1
                    RETURNING id, quantity;
                    """,
                    (cart_id, item_id),
                )
            conn.commit()
        return {"ok": True}

    def get_cart(self, user_id: str) -> dict:
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT id FROM carts WHERE user_id=%s AND status='open' ORDER BY id DESC LIMIT 1;", (user_id,))
                row = cur.fetchone()
                if not row:
                    return {"user_id": user_id, "items": [], "total_amount": 0.0}
                cart_id = row[0]

                cur.execute(
                    """
                    SELECT ci.product_id, ci.quantity
                    FROM cart_items ci
                    WHERE ci.cart_id=%s
                    ORDER BY ci.product_id;
                    """,
                    (cart_id,),
                )
                items_rows = cur.fetchall()

                cur.execute(
                    """
                    SELECT SUM(ci.quantity * p.price)
                    FROM cart_items ci JOIN products p ON p.id = ci.product_id
                    WHERE ci.cart_id=%s;
                    """,
                    (cart_id,),
                )
                total = cur.fetchone()[0]

        items = [{"product_id": r[0], "quantity": r[1]} for r in items_rows]
        return {"user_id": user_id, "items": items, "total_amount": float(total or 0.0)}

    def checkout(self, user_id: str):
        """Create an order from the user's open cart and close the cart."""
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                # Find open cart
                cur.execute("SELECT id FROM carts WHERE user_id=%s AND status='open' ORDER BY id DESC LIMIT 1;", (user_id,))
                row = cur.fetchone()
                if not row:
                    raise ValueError("No open cart")
                cart_id = row[0]

                # Calculate total and snapshot to order_items
                cur.execute(
                    """
                    SELECT ci.product_id, ci.quantity, p.price
                    FROM cart_items ci JOIN products p ON p.id = ci.product_id
                    WHERE ci.cart_id=%s;
                    """,
                    (cart_id,),
                )
                items = cur.fetchall()
                if not items:
                    raise ValueError("Cart is empty")

                cur.execute("INSERT INTO orders (cart_id, user_id, status, total_amount) VALUES (%s, %s, 'created', 0) RETURNING id;", (cart_id, user_id))
                order_id = cur.fetchone()[0]

                total = 0.0
                for product_id, qty, price in items:
                    total += float(qty) * float(price)
                    cur.execute(
                        "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (%s, %s, %s, %s)",
                        (order_id, product_id, qty, price),
                    )

                cur.execute("UPDATE orders SET total_amount=%s WHERE id=%s;", (total, order_id))
                # Close cart
                cur.execute("UPDATE carts SET status='checked_out' WHERE id=%s;", (cart_id,))
            conn.commit()
        # record business metrics
        try:
            self.checkout_counter.labels(service=self.name).inc()
            self.checkout_amount_hist.labels(service=self.name).observe(float(total))
        except Exception:
            pass

        return {"order_id": order_id, "status": "created", "total_amount": total}
