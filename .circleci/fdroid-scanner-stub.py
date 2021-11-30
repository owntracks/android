#!/usr/bin/env python3
#
# fdroid-scanner-stub.py


import re
import subprocess
import logging

CODE_SIGNATURES = {
    # The `apkanalyzer dex packages` output looks like this:
    # M d 1   1       93      <packagename> <other stuff>
    # The first column has P/C/M/F for package, class, method or field
    # The second column has x/k/r/d for removed, kept, referenced and defined.
    # We already filter for defined only in the apkanalyzer call. 'r' will be
    # for things referenced but not distributed in the apk.
    exp: re.compile(r'.[\s]*d[\s]*[0-9]*[\s]*[0-9*][\s]*[0-9]*[\s]*' + exp, re.IGNORECASE) for exp in [
        r'(com\.google\.firebase[^\s]*)',
        r'(com\.google\.android\.gms[^\s]*)',
        r'(com\.google\.android\.play\.core[^\s]*)',
        r'(com\.google\.tagmanager[^\s]*)',
        r'(com\.google\.analytics[^\s]*)',
        r'(com\.android\.billing[^\s]*)',
    ]
}

apkfile = "./project/app/build/outputs/apk/oss/debug/app-oss-debug.apk"

result = subprocess.run(["apkanalyzer", "dex", "packages", "--defined-only", apkfile], capture_output=True, encoding="UTF-8")
if result.returncode != 0:
    logging.warning(f"scanner not cleanly run apkanalyzer: {result.stderr}")
problems = 0
for suspect, regexp in CODE_SIGNATURES.items():
    matches = regexp.findall(result.stdout)
    if matches:
        for match in set(matches):
            logging.warning(f"Found class {match}")
        problems += 1
if problems:
    logging.critical(f"Found {problems} problem classes in {apkfile}")
    exit(1)
