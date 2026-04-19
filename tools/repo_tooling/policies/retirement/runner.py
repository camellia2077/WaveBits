from __future__ import annotations

from .android_private_headers import run_android_private_header_self_contained_policy_checks
from .boundary_hosts import run_boundary_hosts_policy_checks
from .post_legacy_surfaces import run_post_legacy_surfaces_policy_checks
from .retired_wrappers import run_retired_wrappers_policy_checks


def run_retirement_policy_checks() -> None:
    run_boundary_hosts_policy_checks()
    run_retired_wrappers_policy_checks()
    run_android_private_header_self_contained_policy_checks()
    run_post_legacy_surfaces_policy_checks()
