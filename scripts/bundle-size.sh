#!/usr/bin/env bash
# Guard (spec 002, SC-V04): production build has no warnings and initial transfer size < 500 kB.
#   bundle-size.sh            builds the frontend and prints raw + transfer size of the initial bundle
source "$(dirname "$0")/lib.sh"
LOG="$(mktemp)"
(cd frontend && npx ng build) > "$LOG" 2>&1 || { tail -30 "$LOG"; echo "bundle-size: FAIL (build failed)"; exit 1; }
python3 -I - "$LOG" <<'PY'
import re, sys
t = open(sys.argv[1]).read()
m = re.search(r"Initial total\s*\|\s*([\d.]+) (kB|MB)\s*\|\s*([\d.]+) (kB|MB)", t)
if not m: sys.exit("bundle-size: FAIL (no 'Initial total' line in the build output)")
kb = lambda v, u: float(v) * (1000 if u == "MB" else 1)
raw, tr = kb(m[1], m[2]), kb(m[3], m[4])
warn = len(re.findall(r"(?im)^\s*(\x1b\[[0-9;]*m)*\s*(warning|\[warning\])", t)) + len(re.findall(r"(?i)\bwarning\b.*(budget|exceeded)", t))
print(f"bundle-size: initial raw {raw:.1f} kB, transfer {tr:.1f} kB, budget warnings {warn}")
if tr >= 500 or warn: sys.exit("bundle-size: FAIL (transfer must be < 500 kB and the build must print no warnings)")
print("bundle-size: PASS")
PY
