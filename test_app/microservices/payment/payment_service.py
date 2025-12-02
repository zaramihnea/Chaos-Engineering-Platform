from microservices.base_service import BaseService
from common import env as _env


class PaymentService(BaseService):
    def __init__(self, name: str, port: int):
        super().__init__(name, port)
        self.payment_counter = self.metrics.create_counter(
            "payments_total", "Number of processed payments"
        )
        self.payment_failures = self.metrics.create_counter(
            "payments_failed_total", "Number of failed payments"
        )
        self.payment_amount_hist = self.metrics.create_histogram(
            "payment_amount", "Amount of processed payments"
        )

    def process_payment(self, user_id: str, amount: float) -> bool:
        from common.db import get_conn
        try:
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

                    cur.execute(
                        "INSERT INTO payments (order_id, amount, currency, provider, provider_ref, status) VALUES (%s, %s, 'USD', 'mock', 'auto', 'succeeded') RETURNING id;",
                        (order_id, amount),
                    )
                    _payment_id = cur.fetchone()[0]
                    cur.execute("UPDATE orders SET status='paid' WHERE id=%s;", (order_id,))
                conn.commit()
            try:
                self.payment_counter.labels(service=self.name).inc()
                self.payment_amount_hist.labels(service=self.name).observe(float(amount))
            except Exception:
                pass
            return True
        except Exception:
            try:
                self.payment_failures.labels(service=self.name).inc()
            except Exception:
                pass
            raise

    def refund_payment(self, payment_id: str):
        from common.db import get_conn

        with get_conn() as conn:
            with conn.cursor() as cur:
                cur.execute("UPDATE payments SET status='refunded' WHERE id=%s;", (payment_id,))
            conn.commit()
        try:
            self.payment_counter.labels(service=self.name).inc()
        except Exception:
            pass
        return True
