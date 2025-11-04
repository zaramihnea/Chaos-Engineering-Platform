import pytest

from microservices.catalog.catalog_service import CatalogService


@pytest.fixture
def service():
    return CatalogService(name="catalog", port=5001)


def test_list_items_returns_non_empty_list_with_schema(service):
    items = service.list_items()
    assert isinstance(items, list)
    assert len(items) > 0
    for item in items:
        assert isinstance(item, dict)
        assert set(["id", "name", "price"]).issubset(item.keys())
        assert isinstance(item["id"], int)
        assert isinstance(item["name"], str)
        assert isinstance(item["price"], (int, float))
        assert item["price"] >= 0


def test_get_item_by_id_success(service):
    # Assuming at least one item exists with id=1
    item = service.get_item(1)
    assert item["id"] == 1
    assert isinstance(item["name"], str)


def test_get_item_not_found_raises_keyerror(service):
    with pytest.raises(KeyError):
        service.get_item(999999)
