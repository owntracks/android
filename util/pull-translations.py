#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Pull translations from POEditor into the app's existing locales.

Only locales the app already has are updated. Well-translated languages that the app doesn't have
yet are listed at the end, so someone can decide whether to add them: creating the
values-<locale>/strings.xml is all it takes, as the locale config is generated at build time.
"""

import json
import os
import sys
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

API_URL = "https://api.poeditor.com/v2"
PROJECT_ID = "419041"
RES_DIR = Path(__file__).resolve().parent.parent / "project/app/src/main/res"
SOURCE_LANGUAGE = "en"
# POEditor's percentage complete a missing language must exceed to be worth mentioning.
MISSING_LOCALE_THRESHOLD = 50
# Codes that don't convert mechanically: Android uses the legacy codes for Indonesian and Hebrew.
ANDROID_LOCALES = {"zh-CN": "zh", "id": "in", "he": "iw"}


def request(url: str, data: bytes | None = None) -> bytes:
    # Some CDNs reject urllib's default user agent
    req = urllib.request.Request(url, data=data, headers={"User-Agent": "owntracks-pull-translations"})
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


def poeditor(endpoint: str, api_key: str, **fields: str) -> dict[str, Any]:
    """Calls a POEditor endpoint, which reports failures in the body of a 200 response."""
    form = {"api_token": api_key, "id": PROJECT_ID, **fields}
    body = json.loads(request(f"{API_URL}/{endpoint}", urllib.parse.urlencode(form).encode()))
    if body["response"]["status"] != "success":
        sys.exit(f"error: POEditor {endpoint} failed: {body['response']['message']}")
    return body["result"]


def android_locale(code: str) -> str:
    """The Android resource qualifier for a POEditor language code."""
    if code in ANDROID_LOCALES:
        return ANDROID_LOCALES[code]
    language, *rest = code.split("-")
    if not rest:
        return language
    if len(rest) == 1 and len(rest[0]) == 2:
        return f"{language}-r{rest[0].upper()}"
    return "b+" + "+".join([language, *rest])


def strings_file(locale: str) -> Path:
    return RES_DIR / f"values-{locale}" / "strings.xml"


def pull(code: str, destination: Path, api_key: str) -> None:
    export = poeditor(
        "projects/export",
        api_key,
        language=code,
        order="terms",
        filters="translated",
        type="android_strings",
        options=json.dumps([{"unquoted": 0}]),
    )
    content = request(export["url"])
    destination.write_bytes(content if content.endswith(b"\n") else content + b"\n")


def main() -> None:
    api_key = os.environ.get("POEDITOR_API_KEY")
    if not api_key:
        sys.exit("error: set POEDITOR_API_KEY first")

    languages = poeditor("languages/list", api_key)["languages"]
    missing = []
    for language in sorted(languages, key=lambda it: it["code"]):
        code = language["code"]
        if code == SOURCE_LANGUAGE:
            continue
        locale = android_locale(code)
        destination = strings_file(locale)
        if destination.is_file():
            print(f"Fetching {code} into {locale}")
            pull(code, destination, api_key)
        else:
            print(f"Skipping {code} because {locale} doesn't exist")
            if language["percentage"] > MISSING_LOCALE_THRESHOLD:
                missing.append((language, locale))

    if missing:
        print(f"\nOver {MISSING_LOCALE_THRESHOLD}% translated on POEditor, but not in the app:")
        for language, locale in sorted(missing, key=lambda it: it[0]["percentage"], reverse=True):
            print(f"  {language['name']} ({language['code']}): {language['percentage']}%, add values-{locale}")


if __name__ == "__main__":
    main()
