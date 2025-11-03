from microservices.base_service import BaseService

class GatewayService(BaseService):
    def route_request(self, service_name: str, endpoint: str, payload: dict):
        pass

    def handle_response(self, response):
        pass
