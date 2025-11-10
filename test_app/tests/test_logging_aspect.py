import io
import sys
import pytest

from aop.logging_aspect import LoggingAspect


class Dummy:
    def ok(self, x: int):
        return x * 2

    def fail(self):
        raise ValueError("boom")


def test_logging_aspect_ok_and_exception():
    d = Dummy()
    aspect = LoggingAspect(timing=True)
    aspect.apply_to_public_methods(d, include=["ok", "fail"])

    buf = io.StringIO()
    orig_stdout = sys.stdout
    try:
        sys.stdout = buf
        assert d.ok(3) == 6
        with pytest.raises(ValueError):
            d.fail()
    finally:
        sys.stdout = orig_stdout

    out = buf.getvalue()
    assert "before ok" in out
    assert "after ok" in out
    assert "exception in fail" in out