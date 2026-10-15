#!/usr/bin/env bash
# Guard (spec 002, SC-V07): no colour literal outside the token files.
#   colour-check.sh            report offenders, exit 0
#   colour-check.sh --enforce  exit 1 when any offender exists
# Scans frontend/src/app and frontend/src/styles for #hex, rgb()/rgba(), hsl()/hsla() literals, except in
# frontend/src/styles/_palette.scss and frontend/src/styles/_tokens.scss.
source "$(dirname "$0")/lib.sh"
HITS="$(grep -rInE '#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(' frontend/src/app frontend/src/styles \
        --include='*.ts' --include='*.html' --include='*.scss' --include='*.css' 2>/dev/null \
        | grep -vE 'frontend/src/styles/_(palette|tokens)\.scss' | grep -vE '#[0-9]+(\)|[^0-9a-fA-F])?$' | grep -vE "&#|href=\"#|routerLink|\[\^#" || true)"
N=$(printf '%s' "$HITS" | grep -c . || true)
if [[ "$N" -gt 0 ]]; then
  echo "colour-check: $N colour literal(s) outside _palette.scss/_tokens.scss:"; echo "$HITS" | head -40
  [[ "${1:-}" == "--enforce" ]] && exit 1 || exit 0
fi
echo "colour-check: PASS (0 colour literals outside the token files)"
