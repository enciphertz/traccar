"""Version and notes for a production release.

Run by the `release` job of .github/workflows/deploy.yml after a production
deploy succeeds. Writes `release-notes.md` and emits `version` and `range`
to $GITHUB_OUTPUT.

The version is the one given, else the previous `vX.Y.Z` tag with its patch
bumped, else v0.1.0 (the first release cut by this pipeline). The notes cover
everything since the previous release tag -- release to release -- falling
back to the previously deployed commit when there is no tag yet.

A given version may also be a patch release, `vX.Y.Z-patch-release` (then
`-patch-release-2`, `-3` ... for later patches on the same release), the
naming the Petrolink pipeline uses. A patch counts as a release for
where the next notes start, but never as the base of the automatic bump --
after `v0.1.0-patch-release` an empty version is still `v0.1.1`, the next
planned release, never a name derived from the patch.

Usage: release_notes.py <component> <previous_deployed_sha> <deployed_sha> [version]
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from datetime import datetime, timezone

FIRST_VERSION = "v0.1.0"
TAG = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")
# The `-N` counter starts at 2: the first patch carries no number, so a `-1`
# would be a second name for it.
RELEASE = re.compile(r"^v(\d+)\.(\d+)\.(\d+)(-patch-release(?:-([2-9]|[1-9]\d+))?)?$")
KEY = re.compile(r"\bESYNC-\d+\b")
JIRA_BROWSE = "https://enciphertz.atlassian.net/browse/"
MAX_COMMITS_LISTED = 300
MAX_BODY = 120_000


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def known(sha: str) -> bool:
    return bool(sha) and subprocess.run(
        ["git", "cat-file", "-e", f"{sha}^{{commit}}"], capture_output=True
    ).returncode == 0


def is_release_version(name: str) -> bool:
    return RELEASE.match(name) is not None


def version_key(name: str) -> tuple[int, int, int, int]:
    """Sort key: a release, then its patches in order, then the next release."""
    major, minor, patch, is_patch, number = RELEASE.match(name).groups()
    return (int(major), int(minor), int(patch), int(number) if number else 1 if is_patch else 0)


def latest_tag() -> str | None:
    """The newest release of either kind: where the next notes start."""
    tags = [t for t in git("tag", "--list", "v*").split() if is_release_version(t)]
    return max(tags, key=version_key) if tags else None


def latest_plain_tag() -> str | None:
    """The newest `vX.Y.Z`: the base of the automatic bump."""
    tags = [t for t in git("tag", "--list", "v*").split() if TAG.match(t)]
    return max(tags, key=version_key) if tags else None


def next_version(previous: str | None) -> str:
    if previous is None:
        return FIRST_VERSION
    major, minor, patch = (int(x) for x in TAG.match(previous).groups())
    return f"v{major}.{minor}.{patch + 1}"


def main() -> int:
    return main_args(sys.argv[1:])


def main_args(argv: list[str]) -> int:
    component, previous_sha, deployed, *rest = argv
    requested = rest[0].strip() if rest and rest[0].strip() else ""
    if requested and not is_release_version(requested):
        print(
            f"version must look like v1.2.3 or v1.2.3-patch-release[-N], got {requested!r}",
            file=sys.stderr,
        )
        return 2

    previous_tag = latest_tag()
    version = requested or next_version(latest_plain_tag())
    if version in git("tag", "--list", version).split():
        print(f"{version} is already tagged; pass a new version", file=sys.stderr)
        return 2

    base = previous_tag if previous_tag else (previous_sha if known(previous_sha) else "")
    rng = f"{base}..{deployed}" if base else deployed

    commits = [
        line.split(" ", 1)
        for line in git("log", "--no-merges", "--format=%h %s", rng).splitlines()
        if line.strip()
    ]
    bodies = git("log", "--format=%s%n%b", rng)
    keys = sorted(set(KEY.findall(bodies)), key=lambda k: int(k.split("-")[1]))
    migrations = []
    if base:
        # MIGRATION_PATHS: space-separated git pathspecs, set per repo by deploy.yml
        # (Django: ':(glob)**/migrations/*.py', SQL services: 'migrations/*.sql').
        pathspecs = os.environ.get("MIGRATION_PATHS", "").split()
        if pathspecs:
            migrations = [
                path for path in git(
                    "diff", "--name-only", "--diff-filter=A", base, deployed, "--", *pathspecs
                ).splitlines() if not path.endswith("__init__.py")
            ]

    run_url = (
        f"{os.environ.get('GITHUB_SERVER_URL', '')}/{os.environ.get('GITHUB_REPOSITORY', '')}"
        f"/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}"
    )
    lines = [
        f"**{component} {version}** deployed to production on "
        f"{datetime.now(timezone.utc):%Y-%m-%d %H:%M} UTC.",
        "",
        f"- Commit: `{deployed}`",
        f"- Since: {previous_tag or (base[:7] if base else 'the beginning')}",
        f"- Deploy run: {run_url}",
        "",
    ]
    if keys:
        lines += ["## Jira issues", ""] + [f"- [{k}]({JIRA_BROWSE}{k})" for k in keys] + [""]
    if migrations:
        lines += ["## Database migrations", ""] + [f"- `{m}`" for m in migrations] + [""]
    # GitHub caps a release body at 125,000 characters; a first release of a long-lived fork
    # (traccar) has tens of thousands of commits behind it. List the newest, count the rest.
    shown = commits[:MAX_COMMITS_LISTED]
    lines += ["## Changes", ""] + [f"- {subject} (`{sha}`)" for sha, subject in shown]
    if len(commits) > len(shown):
        lines.append(f"- … and {len(commits) - len(shown)} earlier commits (`git log {rng}`)")
    lines.append("")
    lines += [
        "## Rollback",
        "",
        "Re-run Actions -> Deploy on `main` for the previous release's commit (revert on "
        "`main`), or on the box: `echo $GHCR_TOKEN | esync-deploy prod <component> <sha>`. "
        "The previous images are kept on the box. Migrations are additive and are not reversed.",
    ]
    body = "\n".join(lines) + "\n"
    if len(body) > MAX_BODY:
        body = body[:MAX_BODY] + "\n\n… (notes truncated to GitHub's release size limit)\n"
    with open("release-notes.md", "w") as handle:
        handle.write(body)

    with open(os.environ.get("GITHUB_OUTPUT", "/dev/stdout"), "a") as out:
        out.write(f"version={version}\nrange={rng}\n")
    print(f"{version}: {len(commits)} commits, {len(keys)} issues, {len(migrations)} migrations")
    return 0


if __name__ == "__main__":
    sys.exit(main())
