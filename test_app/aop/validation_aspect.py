"""Validation aspect for runtime argument type checking.

This aspect inspects function parameter annotations and performs simple
runtime checks when wrapped. It supports simple types and basic
typing.Union (including Optional). For complex typing constructs the
check is conservative (only checks container types, not element types).

Usage:
    aspect = ValidationAspect()
    aspect.apply_to_public_methods(obj, include=[...])

The aspect skips parameters named 'self'.
"""
from __future__ import annotations

import functools
import inspect
from typing import get_origin, get_args, Union


class ValidationAspect:
    def __init__(self) -> None:
        pass

    def _type_name(self, t: object) -> str:
        try:
            return t.__name__
        except Exception:
            return str(t)

    def _match_type(self, expected: object, value: object) -> bool:
        origin = get_origin(expected)
        if origin is Union:
            # Union[...] or Optional[...] - accept if any option matches
            for arg in get_args(expected):
                try:
                    if self._match_type(arg, value):
                        return True
                except Exception:
                    continue
            return False

        # If expected is a plain type
        if isinstance(expected, type):
            return isinstance(value, expected)

        # If annotation is a typing container like list[int], just check origin
        if origin in (list, tuple, dict, set):
            return isinstance(value, origin)

        # Unknown/complex annotation - be conservative and accept (no strict check)
        return True

    def _validate_call(self, func, bound_args: inspect.BoundArguments) -> None:
        annotations = getattr(func, "__annotations__", {}) or {}
        # BoundArguments stores values in the .arguments mapping
        for name, value in bound_args.arguments.items():
            if name == "self":
                continue
            if name in annotations:
                expected = annotations[name]
                try:
                    ok = self._match_type(expected, value)
                except Exception:
                    ok = False
                if not ok:
                    raise TypeError(
                        f"Argument '{name}' to {func.__name__} expected {self._type_name(expected)}, got {type(value).__name__}"
                    )

    def apply(self, func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            sig = inspect.signature(func)
            try:
                bound = sig.bind_partial(*args, **kwargs)
            except TypeError:
                # Let underlying function raise for missing required args.
                return func(*args, **kwargs)
            # bound.arguments is an ordered mapping name->value
            self._validate_call(func, bound)
            return func(*args, **kwargs)

        return wrapper

    def apply_to_public_methods(self, obj, include: list[str] | None = None, exclude: list[str] | None = None) -> None:
        exclude_set = set(exclude or [])
        for attr in dir(obj):
            if attr.startswith("_"):
                continue
            if include is not None and attr not in include:
                continue
            if attr in exclude_set:
                continue
            try:
                val = getattr(obj, attr)
            except Exception:
                continue
            if callable(val):
                try:
                    wrapped = self.apply(val)
                    setattr(obj, attr, wrapped)
                except Exception:
                    # skip wrapping if anything about the attribute prevents it
                    continue
