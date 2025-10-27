from microservices.base_service import BaseService

class CatalogService(BaseService):
    def list_items(self) -> list:
        pass

    def get_item(self, item_id: int) -> dict:
        pass
