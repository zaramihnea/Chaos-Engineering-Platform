from microservices.base_service import BaseService
from common import env as _env


class PaymentService(BaseService):
    def process_payment(self, user_id: str, amount: float) -> bool:
        """Process payment for the most recent 'created' order of a user.
        
        - Find the latest order with status 'created' for the given user.
        - Insert a succeeded payment linked to that order for the provided amount.
        - Mark the order as 'paid'.
        """
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT id, total_amount FROM orders WHERE user_id=%s AND status='created' ORDER BY id DESC LIMIT 1;",
                    (user_id,),
                )
                row = cur.fetchone()
                if not row:
                    raise ValueError("No order to pay for user")
                order_id, total_amount = row

                # In a real system we'd authorize/capture with provider; here we trust amount
                cur.execute(
                    "INSERT INTO payments (order_id, amount, currency, provider, provider_ref, status) VALUES (%s, %s, 'USD', 'mock', 'auto', 'succeeded') RETURNING id;",
                    (order_id, amount),
                )
                _payment_id = cur.fetchone()[0]
                cur.execute("UPDATE orders SET status='paid' WHERE id=%s;", (order_id,))
            conn.commit()
        return True

    def refund_payment(self, payment_id: str):
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                cur.execute("UPDATE payments SET status='refunded' WHERE id=%s;", (payment_id,))
            conn.commit()
        return True
