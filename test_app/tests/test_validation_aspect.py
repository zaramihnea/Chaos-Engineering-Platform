import pytest

from aop.validation_aspect import ValidationAspect


class Dummy:
    def add(self, x: int, y: int) -> int:
        return x + y

    def echo(self, s: str) -> str:
        return s.upper()

    def untyped(self, a, b):
        return (a, b)


def test_validation_accepts_valid_args():
    d = Dummy()
    aspect = ValidationAspect()
    aspect.apply_to_public_methods(d, include=["add", "echo", "untyped"])

    assert d.add(2, 3) == 5
    assert d.echo("hi") == "HI"
    assert d.untyped("a", 1) == ("a", 1)


def test_validation_rejects_invalid_types():
    d = Dummy()
    aspect = ValidationAspect()
    aspect.apply_to_public_methods(d, include=["add", "echo"])

    with pytest.raises(TypeError):
        d.add("not-an-int", 3)

    with pytest.raises(TypeError):
        d.echo(123)
