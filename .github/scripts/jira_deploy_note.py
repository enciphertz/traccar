"""Tell the Jira issues in a deploy that they were deployed.

Run by .github/workflows/deploy.yml after a successful deploy. Collects the
ESYNC keys from the commit messages between the previously deployed sha and
the new one, comments on each with where it went, and on production moves
it to "Deployed".

The Jira Deployments panel is filled separately and automatically by the
GitHub for Jira app from the job's `environment`; this adds the comment and
the status change that the panel does not. Skips quietly when the Jira
secrets are not configured.

Usage: jira_deploy_note.py <environment> <previous_sha> <deployed_sha> <run_url> [fallback_sha]

`fallback_sha` (the push's `before`) is used when the box was running a
commit GitHub never had -- a merge made by hand on the server.

With JIRA_RELEASE_VERSION set (the release job sets it to the new tag), the
matching version (named "<component> vX.Y.Z", since each E-Sync
repo is released on its own) is created in project ESYNC if missing, marked released
today, and added as a fix version on every issue in the range -- so Jira's
Releases view lists exactly what each production release shipped.
"""
from __future__ import annotations

import base64
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request

KEY = re.compile(r"\bESYNC-\d+\b")
PROJECT = "ESYNC"
DEPLOYED_STATUS = "Deployed"


def jira(method: str, path: str, body: dict | None = None) -> dict:
    base = os.environ["JIRA_BASE_URL"].rstrip("/")
    token = base64.b64encode(
        f"{os.environ['JIRA_EMAIL']}:{os.environ['JIRA_API_TOKEN']}".encode()
    ).decode()
    request = urllib.request.Request(
        f"{base}/rest/api/3/{path}",
        method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={
            "Authorization": f"Basic {token}",
            "Accept": "application/json",
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        raw = response.read()
    return json.loads(raw) if raw else {}


def keys_between(previous: str, deployed: str) -> list[str]:
    if previous == deployed:
        return []
    log = subprocess.run(
        ["git", "log", "--format=%B", f"{previous}..{deployed}"],
        check=True, capture_output=True, text=True,
    ).stdout
    return sorted(set(KEY.findall(log)), key=lambda k: int(k.split("-")[1]))


def comment(key: str, text: str) -> None:
    jira("POST", f"issue/{key}/comment", {
        "body": {"type": "doc", "version": 1, "content": [
            {"type": "paragraph", "content": [{"type": "text", "text": text}]}
        ]}
    })


def move_to_deployed(key: str) -> None:
    issue = jira("GET", f"issue/{key}?fields=status")
    if issue["fields"]["status"]["name"] in (DEPLOYED_STATUS, "Done"):
        return
    transitions = jira("GET", f"issue/{key}/transitions")["transitions"]
    target = next((t for t in transitions if t["to"]["name"] == DEPLOYED_STATUS), None)
    if target is not None:
        jira("POST", f"issue/{key}/transitions", {"transition": {"id": target["id"]}})


def ensure_released_version(name: str) -> str:
    """The id of Jira version `name` in ESYNC, created (and marked released
    today) if it does not exist yet."""
    from datetime import date

    for version in jira("GET", f"project/{PROJECT}/versions"):
        if version["name"] == name:
            if not version.get("released"):
                jira("PUT", f"version/{version['id']}", {
                    "released": True, "releaseDate": date.today().isoformat(),
                })
            return version["id"]
    project_id = jira("GET", f"project/{PROJECT}")["id"]
    created = jira("POST", "version", {
        "name": name, "projectId": int(project_id), "released": True,
        "releaseDate": date.today().isoformat(),
        "description": "Production release, created by the deploy pipeline.",
    })
    return created["id"]


def add_fix_version(key: str, version_id: str) -> None:
    jira("PUT", f"issue/{key}", {"update": {"fixVersions": [{"add": {"id": version_id}}]}})


def known(sha: str) -> bool:
    return bool(sha) and subprocess.run(
        ["git", "cat-file", "-e", f"{sha}^{{commit}}"], capture_output=True
    ).returncode == 0


def main() -> int:
    environment, previous, deployed, run_url = sys.argv[1:5]
    fallback = sys.argv[5] if len(sys.argv) > 5 else ""
    if not known(previous):
        previous = fallback if known(fallback) else f"{deployed}~1"
    if not all(os.environ.get(v) for v in ("JIRA_BASE_URL", "JIRA_EMAIL", "JIRA_API_TOKEN")):
        print("Jira secrets not set; skipping issue updates.")
        return 0
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    release = os.environ.get("JIRA_RELEASE_VERSION", "").strip()
    keys = keys_between(previous, deployed)
    print(f"Issues in {previous[:7]}..{deployed[:7]}: {', '.join(keys) or 'none'}")
    version_id = None
    if release:
        try:
            version_id = ensure_released_version(release)
            print(f"Jira version {release} ready ({version_id})")
        except urllib.error.HTTPError as error:
            print(f"could not create Jira version {release}: {error.code}")
    label = f"{environment} as {release}" if release else environment
    for key in keys:
        try:
            comment(key, f"Deployed to {label}: {repo}@{deployed[:7]} ({run_url})")
            if version_id:
                add_fix_version(key, version_id)
            if environment == "production":
                move_to_deployed(key)
        except urllib.error.HTTPError as error:
            # One unreadable issue (a key reissued, a typo in a commit) must
            # not fail a deploy that already succeeded.
            print(f"{key}: Jira returned {error.code}; skipped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
