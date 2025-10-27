from microservices.base_service import BaseService

class CartService(BaseService):
    def add_to_cart(self, user_id: str, item_id: int):
        pass

    def get_cart(self, user_id: str) -> dict:
        pass

    def checkout(self, user_id: str):
        pass
