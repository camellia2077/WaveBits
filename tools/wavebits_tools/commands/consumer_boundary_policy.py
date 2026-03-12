from __future__ import annotations

# Backward-compatible shim. Prefer boundary_policy.py.
from .boundary_policy import run_consumer_boundary_policy_checks

__all__ = ["run_consumer_boundary_policy_checks"]
