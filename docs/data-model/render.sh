#!/usr/bin/env bash
#
# render.sh — Regenerate static PNG exports of every diagram in README.md.
#
# GitHub renders the Mermaid blocks natively, so these exports are only needed
# where Mermaid cannot render: PDF submissions, slide decks, printed material.
#
# Requires Node.js. Installs the Mermaid CLI on first run if absent.
#
#   ./render.sh

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

if ! command -v mmdc >/dev/null 2>&1; then
  echo "Installing @mermaid-js/mermaid-cli..."
  npm install -g @mermaid-js/mermaid-cli
fi

mkdir -p rendered
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Chromium refuses to start as root without --no-sandbox. This is common in
# CI runners and containers, so supply the flag when it applies.
MMDC_OPTS=()
if [ "$(id -u)" -eq 0 ]; then
  cat > "$WORK/puppeteer.json" <<'JSON'
{ "args": ["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"] }
JSON
  MMDC_OPTS+=(-p "$WORK/puppeteer.json")
  echo "Running as root — passing --no-sandbox to Chromium."
fi

# Diagram order must match the order of ```mermaid blocks in README.md.
NAMES=(
  "01-er-incomeverification"
  "02-er-decisions"
  "03-er-cross-database"
  "04-data-lineage"
)

# Split README.md into one .mmd file per fenced mermaid block.
awk -v out="$WORK" '
  /^```mermaid$/ { inblock=1; n++; next }
  /^```$/ && inblock { inblock=0; next }
  inblock { print > (out "/block" n ".mmd") }
' README.md

COUNT=$(find "$WORK" -name 'block*.mmd' | wc -l)
if [ "$COUNT" -ne "${#NAMES[@]}" ]; then
  echo "ERROR: found $COUNT mermaid blocks but ${#NAMES[@]} names are configured."
  echo "Update the NAMES array in this script to match README.md."
  exit 1
fi

for i in $(seq 1 "$COUNT"); do
  name="${NAMES[$((i-1))]}"
  printf '  %-40s ' "$name"
  mmdc -i "$WORK/block$i.mmd" -o "rendered/$name.png" \
       -t neutral -b white -w 1800 --quiet "${MMDC_OPTS[@]}"
  echo "ok"
done

echo ""
echo "Wrote $COUNT diagrams to rendered/"
