"""Environment loader for the test app.

Import this module as early as possible in entry points to load variables from a .env file.
Does not override variables already present in the environment.
"""
from __future__ import annotations

from dotenv import load_dotenv, find_dotenv

# Find nearest .env from current working directory upwards
# override=False ensures existing environment variables win (e.g., CI secrets)
load_dotenv(find_dotenv(usecwd=True), override=False)
