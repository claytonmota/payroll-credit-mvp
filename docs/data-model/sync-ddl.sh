#!/usr/bin/env bash
#
# sync-ddl.sh — Keep the DDL shown in README.md identical to the .sql files.
#
# The DDL appears in two places: the schema/*.sql files, which are the source
# of truth, and README.md section 3, which quotes them so a reader browsing on
# GitHub does not have to click through. A quotation that is maintained by hand
# rots. This script regenerates the quotation from the source.
#
# Comment lines are stripped when inlining. The full annotations stay in the
# .sql files; the README carries prose instead.
#
#   ./sync-ddl.sh           rewrite README.md from the .sql files
#   ./sync-ddl.sh --check   exit 1 if README.md is out of date, change nothing
#
# Run from anywhere; paths resolve relative to this script.

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
README="$DIR/README.md"
MODE="${1:-write}"

if [ ! -f "$README" ]; then
  echo "ERROR: $README not found." >&2
  exit 1
fi

# Emit a .sql file with comment-only lines removed, blank runs collapsed,
# and leading/trailing blank lines trimmed.
extract_sql() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "ERROR: referenced file not found: $file" >&2
    exit 1
  fi
  # Drop comment-only lines, then drop every blank line — the .sql files put a
  # blank line above each annotated column, and stripping the annotations would
  # otherwise leave the statement full of gaps. A single blank line is then
  # reinserted before each new statement.
  grep -v '^[[:space:]]*--' "$file" | awk '
    /^[[:space:]]*$/ { next }
    {
      if (emitted > 0 && $0 ~ /^(CREATE|ALTER|DROP|COMMENT|INSERT)/) print ""
      print
      emitted++
    }'
}

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

FOUND=0
SKIPPING=0

while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    '<!-- BEGIN-DDL: '*' -->')
      rel="${line#<!-- BEGIN-DDL: }"
      rel="${rel% -->}"
      FOUND=$((FOUND + 1))
      printf '%s\n' "$line"
      printf '```sql\n'
      printf -- '-- source: %s\n' "$rel"
      printf -- '-- annotations omitted here; see the file for full commentary\n\n'
      extract_sql "$DIR/$rel"
      printf '```\n'
      SKIPPING=1
      ;;
    '<!-- END-DDL -->')
      printf '%s\n' "$line"
      SKIPPING=0
      ;;
    *)
      [ "$SKIPPING" -eq 1 ] || printf '%s\n' "$line"
      ;;
  esac
done < "$README" > "$TMP"

if [ "$FOUND" -eq 0 ]; then
  echo "ERROR: no <!-- BEGIN-DDL: ... --> markers found in README.md." >&2
  echo "       Nothing to sync. Has the file been edited?" >&2
  exit 1
fi

if [ "$SKIPPING" -eq 1 ]; then
  echo "ERROR: a BEGIN-DDL marker was never closed by <!-- END-DDL -->." >&2
  exit 1
fi

if [ "$MODE" = "--check" ]; then
  if diff -q "$README" "$TMP" >/dev/null; then
    echo "README.md is in sync with $FOUND schema file(s)."
    exit 0
  fi
  echo "README.md is OUT OF DATE with the .sql files."
  echo ""
  diff -u "$README" "$TMP" | sed -n '1,60p'
  echo ""
  echo "Run ./sync-ddl.sh to update it."
  exit 1
fi

if diff -q "$README" "$TMP" >/dev/null; then
  echo "README.md already in sync with $FOUND schema file(s). Nothing changed."
else
  cp "$TMP" "$README"
  echo "README.md updated from $FOUND schema file(s)."
  echo "Review the diff before committing:  git diff docs/data-model/README.md"
fi
