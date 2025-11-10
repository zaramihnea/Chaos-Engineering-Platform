"""Environment loader for the test app.

Import this module as early as possible in entry points to load variables from a .env file.
Does not override variables already present in the environment.
"""
from __future__ import annotations

from dotenv import load_dotenv, find_dotenv

load_dotenv(find_dotenv(usecwd=True), override=False)
