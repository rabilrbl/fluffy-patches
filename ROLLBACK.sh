#!/usr/bin/env bash
set -euo pipefail

SOURCE="$(cd "$(dirname "$0")" && pwd)/patches/src/main/kotlin/app/template/patches/blockerx/premium/EnablePremiumPatch.kt"
BACKUP="${1:-/tmp/EnablePremiumPatch.rollback-test.kt}"

if [[ ! -f "$BACKUP" ]]; then
    printf 'backup not found: %s\n' "$BACKUP" >&2
    exit 1
fi
cp "$BACKUP" "$SOURCE"
printf 'restored %s from %s\n' "$SOURCE" "$BACKUP"
