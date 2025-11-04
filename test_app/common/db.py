"""Simple Postgres connection helper using pg8000 (Python 3.14 friendly).

Relies on DATABASE_URL from environment (loaded via common.env).
Uses pg8000 under the hood and provides a context-managed connection.
"""
from __future__ import annotations

import os
from contextlib import contextmanager
from urllib.parse import urlparse

import pg8000


def _parse_dsn(dsn: str) -> dict:
    """Parse a PostgreSQL DSN into pg8000 connect kwargs."""
    u = urlparse(dsn)
    if u.scheme not in ("postgresql", "postgres"):
        raise ValueError("Unsupported DSN scheme: " + u.scheme)
    # Username and password
    user = u.username
    password = u.password
    host = u.hostname or "localhost"
    port = int(u.port) if u.port else 5432
    # path starts with '/'
    database = (u.path[1:] if u.path and len(u.path) > 1 else None) or os.getenv("PGDATABASE")
    if not database:
        raise ValueError("Database name missing in DSN and PGDATABASE not set")
    return {
        "user": user,
        "password": password,
        "host": host,
        "port": port,
        "database": database,
    }


@contextmanager
def get_conn():
    dsn = os.getenv("DATABASE_URL")
    if not dsn:
        raise RuntimeError("DATABASE_URL is not set in environment")
    kwargs = _parse_dsn(dsn)
    conn = pg8000.connect(**kwargs)
    try:
        yield conn
        # pg8000 is autocommit=False by default; callers commit when needed
    finally:
        conn.close()


def db_available() -> bool:
    """Return True if DATABASE_URL is set (driver is pure Python)."""
    return bool(os.getenv("DATABASE_URL"))
