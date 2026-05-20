#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
EMBEDDED_DIR="$ROOT_DIR/public/embedded"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

check_tool() {
    if ! command -v "$1" &>/dev/null; then
        echo -e "${RED}✗ $1 not found. Install with: npm install -g $2${NC}"
        exit 1
    fi
}

check_tool "terser" "terser"
check_tool "cleancss" "clean-css-cli"
check_tool "html-minifier-terser" "html-minifier-terser"

echo "Minifying embedded assets..."
echo ""

# --- CSS ---
for f in "$EMBEDDED_DIR"/css/*.css; do
    [[ "$f" == *.min.css ]] && continue
    basename=$(basename "$f")
    out="${f%.css}.min.css"
    cleancss -o "$out" "$f"
    original=$(wc -c < "$f" | tr -d ' ')
    minified=$(wc -c < "$out" | tr -d ' ')
    ratio=$((100 - minified * 100 / original))
    echo -e "${GREEN}CSS${NC}  $basename  ${original}B → ${minified}B  (${YELLOW}-${ratio}%${NC})"
done

# --- JS ---
for f in "$EMBEDDED_DIR"/js/*.js; do
    [[ "$f" == *.min.js ]] && continue
    basename=$(basename "$f")
    out="${f%.js}.min.js"
    terser "$f" --compress --mangle -o "$out"
    original=$(wc -c < "$f" | tr -d ' ')
    minified=$(wc -c < "$out" | tr -d ' ')
    ratio=$((100 - minified * 100 / original))
    echo -e "${GREEN}JS ${NC}  $basename  ${original}B → ${minified}B  (${YELLOW}-${ratio}%${NC})"
done

# --- HTML ---
for f in "$EMBEDDED_DIR"/html/*.html; do
    [[ "$f" == *.min.html ]] && continue
    basename=$(basename "$f")
    out="${f%.html}.min.html"
    html-minifier-terser \
        --collapse-whitespace \
        --remove-comments \
        --remove-redundant-attributes \
        --remove-optional-tags \
        --minify-css true \
        --minify-js true \
        -o "$out" "$f"
    original=$(wc -c < "$f" | tr -d ' ')
    minified=$(wc -c < "$out" | tr -d ' ')
    ratio=$((100 - minified * 100 / original))
    echo -e "${GREEN}HTML${NC} $basename  ${original}B → ${minified}B  (${YELLOW}-${ratio}%${NC})"
done

echo ""
echo -e "${GREEN}Done!${NC}"
