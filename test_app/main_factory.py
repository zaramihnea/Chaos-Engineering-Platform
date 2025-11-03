from microservices.catalog.catalog_service import CatalogService
from microservices.cart.cart_service import CartService
from microservices.gateway.gateway_service import GatewayService
from microservices.payment.payment_service import PaymentService

class ServiceFactory:
    def create_service(self, service_type: str):
        pass
