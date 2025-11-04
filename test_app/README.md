```powershell
./.venv/Scripts/python.exe -m pytest -q
```

## Iteration 1 (TDD services)

- Services
	- `microservices/catalog/catalog_service.py`
	- `microservices/cart/cart_service.py`
	- `microservices/payment/payment_service.py`
	- `microservices/gateway/gateway_service.py`
- Common
	- `common/env.py`
	- `common/db.py`
- Tests
	- `tests/test_catalog_service.py`
	- `tests/test_catalog_pg.py`
	- `tests/test_cart_service.py`
	- `tests/test_payment_service.py`
	- `tests/test_gateway_service.py`

## Iteration 2 (TDD routers)

- Routers implemented (Flask):
	- `microservices/catalog/app.py`
	- `microservices/cart/app.py`
	- `microservices/payment/app.py`
	- `microservices/gateway/app.py`

- Tests:
	- `tests/test_catalog_http.py`:
		- `test_http_get_items_matches_db` – verifies `GET /items` equals DB rows.
		- `test_http_get_item_by_id_and_404` – verifies 200 for existing and 404 for missing.
	- `tests/test_cart_http.py` (user `bob`):
		- `test_http_add_get_checkout_bob` – add twice, list cart (quantity increments), checkout returns `order_id` and `status='created'`.
	- `tests/test_payment_http.py` (user `pam`):
		- `test_http_payment_process_marks_paid` – creates an order, POST `/payment/process`, asserts response and DB mark paid.
	- `tests/test_gateway_http.py` (user `gary`):
		- `test_http_gateway_route_checkout_and_pay` – via `POST /route` add item then `checkout_and_pay`; expects `status='paid'` and `order_id`.
