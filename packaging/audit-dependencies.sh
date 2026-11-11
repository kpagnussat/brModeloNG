#!/usr/bin/env bash
set -euo pipefail
# Checks every third-party library against the OSV vulnerability database and fails when
# one has a known vulnerability. gradle/verification-metadata.xml is the input: strict
# dependency verification makes it list every artifact the build resolves.
version=2.6.0
sha256=ca69b3d3cd08f889a49dc0a383122f71cc528b83803671df5fd874d97485b108 # osv-scanner_linux_amd64
root=$(cd "$(dirname "$0")/.." && pwd)
tools=$(mktemp -d "${TMPDIR:-/tmp}/osv-scanner.XXXXXX")
trap 'rm -rf "$tools"' EXIT
curl -fsSL -o "$tools/osv-scanner" "https://github.com/google/osv-scanner/releases/download/v$version/osv-scanner_linux_amd64"
echo "$sha256  $tools/osv-scanner" | sha256sum -c --quiet
chmod +x "$tools/osv-scanner"
"$tools/osv-scanner" scan source -L "$root/gradle/verification-metadata.xml"
