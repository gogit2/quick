#!/usr/bin/env python3
"""Stand-in for `claude -p` used only by dry runs (QF_DRY_RUN=1). It never calls a model.

Dev roles tick up to TASKS_PER_ITERATION open tasks and report progress/built.
verify writes a verdict taken from QF_FAKE_VERDICTS (comma list consumed per round, default PASS).
QF_FAKE_STUCK=<role> makes that role report "stuck" every time.
Prints a JSON object shaped like `claude -p --output-format json`.
"""
import json
import os
import re
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
role, ms = sys.argv[1], sys.argv[2]
sid = str(uuid.uuid4())

if role in ("backend-dev", "frontend-dev"):
    state = ROOT / "loops" / role / "state"
    if os.environ.get("QF_FAKE_STUCK") == role:
        (state / "result.json").write_text(json.dumps({"milestone": ms, "result": "stuck", "tasks_done": [], "open_tasks": -1,
                                                       "selfcheck": "n/a", "notes": "fake stuck"}))
    else:
        f = sorted((ROOT / "loops" / role / "outputs").glob(f"{ms}-*.md"))[0]
        lines, done = f.read_text().splitlines(), []
        for i, line in enumerate(lines):
            m = re.match(r"^(\s*- )\[ \] ((T\d+|FIX-\d+)\b.*)$", line)
            if m and len(done) < 5:
                lines[i] = f"{m.group(1)}[x] {m.group(2)} - done: dry run"
                done.append(m.group(3))
        f.write_text("\n".join(lines) + "\n")
        left = sum(1 for l in lines if re.match(r"^\s*- \[ \] (T\d+|FIX-\d+)", l))
        (state / "result.json").write_text(json.dumps({"milestone": ms, "result": "built" if left == 0 else "progress",
                                                       "tasks_done": done, "open_tasks": left, "selfcheck": "dry run", "notes": ""}))
        with open(ROOT / "loops" / role / "progress.md", "a") as fh:
            fh.write(f"- dry-run · {ms} · ticked {', '.join(done) or 'nothing'}\n")
elif role == "verify":
    counter = ROOT / "run-artifacts" / "logs" / ".fake_verify_round"
    n = int(counter.read_text()) if counter.exists() else 0
    counter.write_text(str(n + 1))
    plan = os.environ.get("QF_FAKE_VERDICTS", "PASS").split(",")
    verdict = plan[min(n, len(plan) - 1)]
    v = {"milestone": ms, "round": n + 1, "verdict": verdict, "checks": {}, "fixes": [], "contract_issues": [], "escalate": False,
         "fix_tasks": [] if verdict == "PASS" else [{"role": "frontend-dev", "text": "dry-run fix task", "evidence": "n/a"}],
         "notes": "dry run"}
    (ROOT / "loops" / "verify" / "state" / "verdict.json").write_text(json.dumps(v))

print(json.dumps({
    "type": "result", "subtype": "success", "is_error": False, "duration_ms": 1234, "num_turns": 7,
    "result": "dry run", "session_id": sid, "total_cost_usd": 0.0123,
    "usage": {"input_tokens": 1500, "output_tokens": 800, "cache_read_input_tokens": 42000, "cache_creation_input_tokens": 9000},
    "modelUsage": {"fake-model": {}},
}))
