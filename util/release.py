#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Tag a stable release, first committing the matching latest-version.yaml.

F-Droid and IzzyOnDroid read that file at the tag to reproduce the build. The version code only
exists on the Play store, so it can't be derived from the source. It has to be the code that
.github/workflows/release.yaml will promote: whichever of the last beta and the internal track is
newer. So with no app changes since the last beta, that beta ships; otherwise the newer internal
build (built from master) ships without needing another beta. release.yaml fails the release if
the file doesn't match, so a wrong answer here costs a re-tag rather than a broken reproducible
build.

Betas are deliberately not supported: the file describes the latest stable release.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

REPO = "owntracks/android"
VERSION_FILE = Path("latest-version.yaml")
GRADLE_FILE = Path("project/app/build.gradle.kts")
# What a promoted beta was built from. A change here means the tag isn't the build Play will ship.
APP_PATHS = [
    "project/app/src/main",
    "project/app/build.gradle.kts",
    "project/gradle/libs.versions.toml",
]


def run(*command: str) -> str:
    """Runs a command, returning its stdout. Exits with its stderr if it fails."""
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit(f"error: {' '.join(command)} failed:\n{result.stderr or result.stdout}")
    return result.stdout.strip()


def succeeds(*command: str) -> bool:
    return subprocess.run(command, capture_output=True).returncode == 0


def latest_beta(tag: str) -> str | None:
    tags = run("git", "tag", "--list", f"{tag}-beta*", "--sort=-v:refname").splitlines()
    return tags[0] if tags else None


def branch_problem() -> str | None:
    if run("git", "branch", "--show-current") != "master":
        return "not on master"
    if run("git", "status", "--porcelain"):
        return "working tree is not clean"
    if run("git", "rev-parse", "HEAD") != run("git", "rev-parse", "origin/master"):
        return "master is not level with origin/master"
    return None


def app_code_changed_since(beta: str) -> bool:
    return not succeeds("git", "diff", "--quiet", beta, "HEAD", "--", *APP_PATHS)


def version_code_of_beta(beta: str) -> str | None:
    release = json.loads(run("gh", "release", "view", beta, "--repo", REPO, "--json", "assets"))
    for asset in release["assets"]:
        if match := re.fullmatch(r"owntracks-release-gms-(\d+)\.apk", asset["name"]):
            return match[1]
    return None


def version_code_of_internal_track() -> str | None:
    if not os.environ.get("ANDROID_PUBLISHER_CREDENTIALS"):
        sys.exit("error: reading the internal track's version code needs ANDROID_PUBLISHER_CREDENTIALS")
    output = run("bundle", "exec", "fastlane", "get_version_code", "track:internal")
    match = re.search(r"Found '(\d+)'", output)
    return match[1] if match else None


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("tag", help="stable tag, like v2.6.0")
    parser.add_argument("--dry-run", action="store_true", help="report what would happen")
    parser.add_argument(
        "--internal-version-code",
        help="the Play internal track's version code, instead of reading it with fastlane "
        "(which needs ANDROID_PUBLISHER_CREDENTIALS); release.yaml still checks it",
    )
    args = parser.parse_args()
    tag: str = args.tag
    dry_run: bool = args.dry_run

    if not re.fullmatch(r"v\d+\.\d+\.\d+", tag):
        sys.exit(f"error: expected a stable tag like v2.6.0, got '{tag}'")
    version = tag.removeprefix("v")

    os.chdir(run("git", "rev-parse", "--show-toplevel"))

    gradle_version = re.search(r'^val versionNameValue = "(.*)"$', GRADLE_FILE.read_text(), re.MULTILINE)
    if gradle_version is None or gradle_version[1] != version:
        found = gradle_version[1] if gradle_version else "nothing"
        sys.exit(f"error: {GRADLE_FILE} has versionName '{found}', not '{version}'")

    run("git", "fetch", "--quiet", "origin", "master", "--tags")
    if run("git", "tag", "--list", tag):
        sys.exit(f"error: tag {tag} already exists")

    def complain(problem: str) -> None:
        """A dry run reports everything it finds, rather than stopping at the first problem."""
        if not dry_run:
            sys.exit(f"error: {problem}")
        print(f"warning: {problem}", file=sys.stderr)

    if problem := branch_problem():
        complain(problem)

    internal_code = args.internal_version_code or version_code_of_internal_track()
    if not re.fullmatch(r"\d+", internal_code or ""):
        sys.exit(f"error: internal track version code '{internal_code}' is not a number")
    source = "Play internal track"
    version_code = internal_code
    if beta := latest_beta(tag):
        beta_code = version_code_of_beta(beta)
        if beta_code is None:
            sys.exit(f"error: no version code found from {beta} APK")
        # Same rule as release.yaml: the newer build ships, and the beta wins a tie.
        if int(beta_code) >= int(internal_code):
            # Promoting the beta ships its build, but F-Droid builds this tag under the same code.
            if app_code_changed_since(beta):
                complain(
                    f"app code changed since {beta} but the internal track has nothing newer: "
                    "wait for the trunk build to upload master to internal"
                )
            source = f"{beta} APK"
            version_code = beta_code

    print(f"{tag}: versionCode {version_code} (from {source})")
    if dry_run:
        return

    answer = input(f"Commit {VERSION_FILE}, tag {tag} and push to origin? [y/N] ")
    if answer != "y":
        sys.exit("error: aborted, nothing changed")

    VERSION_FILE.write_text(f"versionName: {version}\nversionCode: {version_code}\n")
    if not succeeds("git", "diff", "--quiet", "--", str(VERSION_FILE)):
        run("git", "add", str(VERSION_FILE))
        run("git", "commit", "--message", f"chore: bump latest-version for fdroid to {version}")
    run("git", "tag", tag)
    run("git", "push", "--atomic", "origin", "master", tag)
    print(f"Pushed {tag}")


if __name__ == "__main__":
    main()
