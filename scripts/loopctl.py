#!/usr/bin/env python3
"""loopctl: state, metrics and reporting for the QuickFlow loops.

The bash runner calls this for every state change. All writes go through a file lock,
so backend-dev and frontend-dev can run in parallel.

State files
  loops/_shared/milestones.json      milestone-level state (owned by the runner)
  loops/<role>/state/state.json      the role's view of its current milestone (mirror)
  loops/<role>/progress.md           runner table (regenerated) + agent action log (kept)
  run-artifacts/sessions.csv         one row per headless session
  run-artifacts/manual-prompts.csv   prompts of interactive sessions

Run `loopctl.py -h` for the subcommands.
"""
from __future__ import annotations

import argparse
import contextlib
import csv
import fcntl
import json
import math
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SHARED = ROOT / "loops" / "_shared"
MS_FILE = SHARED / "milestones.json"
LOCK_FILE = SHARED / ".state.lock"
SESSIONS_CSV = ROOT / "run-artifacts" / "sessions.csv"
MANUAL_CSV = ROOT / "run-artifacts" / "manual-prompts.csv"
ROLES = ("backend-dev", "frontend-dev", "verify")
DEV_ROLES = ("backend-dev", "frontend-dev")

SESSION_FIELDS = [
    "timestamp", "spec", "milestone", "role", "trial", "iteration", "round", "model",
    "session_id", "prompt_file", "input_tokens", "output_tokens", "cache_read_tokens",
    "cache_write_tokens", "cost_usd", "duration_ms", "num_turns", "is_error", "result", "log_file",
]
MANUAL_FIELDS = ["timestamp", "phase", "session_id", "model", "prompt", "notes"]


# ----------------------------------------------------------------------------- helpers
def now() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def env_cfg() -> dict:
    cfg = {}
    for line in (SHARED / "budget.env").read_text().splitlines():
        line = line.split("#", 1)[0].strip()
        if "=" in line:
            k, v = line.split("=", 1)
            cfg[k.strip()] = v.strip().strip('"')
    return cfg


CFG = env_cfg()


@contextlib.contextmanager
def locked():
    LOCK_FILE.touch(exist_ok=True)
    with open(LOCK_FILE, "r+") as fh:
        fcntl.flock(fh, fcntl.LOCK_EX)
        try:
            yield
        finally:
            fcntl.flock(fh, fcntl.LOCK_UN)


def load() -> dict:
    if not MS_FILE.exists():
        return {"spec": None, "order": [], "milestones": {}}
    data = json.loads(MS_FILE.read_text())
    data.setdefault("order", sorted(data.get("milestones", {})))
    return data


def save(data: dict) -> None:
    tmp = MS_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, indent=2) + "\n")
    tmp.replace(MS_FILE)


def new_role_entry(tasks: int = 0) -> dict:
    return {
        "tasks": tasks, "status": "pending", "iterations": 0, "attempt_iterations": 0,
        "attempt_cap": None, "started_at": None, "ended_at": None,
        "tokens_in": 0, "tokens_out": 0, "cache_read": 0, "cache_write": 0,
        "cost_usd": 0.0, "sessions": [], "models": [], "last_result": None,
    }


def ms_entry(data: dict, ms: str) -> dict:
    if ms not in data["milestones"]:
        sys.exit(f"loopctl: unknown milestone {ms}")
    m = data["milestones"][ms]
    m.setdefault("roles", {})
    for r in ROLES:
        m["roles"].setdefault(r, new_role_entry())
    m.setdefault("events", [])
    return m


def budget_limit(ms: str, upgraded: bool) -> float:
    if upgraded:
        return float(CFG["BUDGET_UPGRADE_USD"])
    large = CFG.get("BUDGET_LARGE_MILESTONES", "").split()
    return float(CFG["BUDGET_LARGE_USD"] if ms in large else CFG["BUDGET_DEFAULT_USD"])


def budget_left(m: dict, ms: str) -> float:
    spent = m.get("upgrade_cost_usd", 0.0) if m.get("upgraded") else m.get("cost_usd", 0.0)
    return round(budget_limit(ms, bool(m.get("upgraded"))) - spent, 4)


def model_for(m: dict, role: str) -> str:
    if role == "verify":
        return CFG["MODEL_VERIFY"]
    return CFG["MODEL_UPGRADE"] if m.get("upgraded") else CFG["MODEL_DEV"]


def event(m: dict, typ: str, role: str, detail: str) -> None:
    m["events"].append({"at": now(), "type": typ, "role": role, "detail": detail})


def milestone_file(role: str, ms: str) -> Path | None:
    hits = sorted((ROOT / "loops" / role / "outputs").glob(f"{ms}-*.md"))
    return hits[0] if hits else None


TASK_RE = re.compile(r"^\s*- \[( |x|X)\] (T\d+|FIX-\d+)\b(.*)$")


def count_tasks(role: str, ms: str, ref: str | None = None) -> tuple[int, int]:
    """Returns (open, total) tasks in the role's milestone file (FIX tasks included)."""
    f = milestone_file(role, ms)
    if not f:
        return (0, 0)
    open_, total = 0, 0
    for line in f.read_text().splitlines():
        mt = TASK_RE.match(line)
        if not mt:
            continue
        if ref and not mt.group(2).startswith("FIX") and f"[{ref}]" not in line:
            continue
        total += 1
        if mt.group(1) == " ":
            open_ += 1
    return open_, total


def attempt_cap(open_tasks: int) -> int:
    per = int(CFG.get("TASKS_PER_ITERATION", 5))
    return math.ceil(max(open_tasks, 1) / per) + int(CFG.get("ITERATION_SLACK", 3))


def write_role_state(data: dict, role: str, ms: str) -> None:
    m = ms_entry(data, ms)
    r = m["roles"][role]
    st = {
        "spec": m.get("spec") or data.get("spec"), "milestone": ms, "milestone_title": m.get("title"),
        "milestone_status": m.get("status"), "status": r["status"], "trial": m.get("trial", 0),
        "upgraded": m.get("upgraded", False), "model": model_for(m, role),
        "iteration": r["iterations"], "attempt_iterations": r["attempt_iterations"],
        "attempt_cap": r["attempt_cap"], "budget_left_usd": budget_left(m, ms),
        "last_session_id": r["sessions"][-1] if r["sessions"] else None,
        "last_result": r.get("last_result"), "updated_at": now(),
    }
    d = ROOT / "loops" / role / "state"
    d.mkdir(parents=True, exist_ok=True)
    (d / "state.json").write_text(json.dumps(st, indent=2) + "\n")


# ----------------------------------------------------------------------------- progress.md
PROG_START, PROG_END = "<!-- runner:start -->", "<!-- runner:end -->"


def fmt_tokens(n: int) -> str:
    return f"{n/1e6:.2f}M" if n >= 1e6 else (f"{n/1e3:.1f}k" if n >= 1e3 else str(n))


def render_progress(data: dict, role: str) -> None:
    path = ROOT / "loops" / role / "progress.md"
    rows = [
        "| Milestone | Status | Start | End | Trials | Iterations | Model(s) | Tokens in / out | Cache read / write | Cost USD | Session ids |",
        "|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    for ms in data.get("order", []):
        m = data["milestones"][ms]
        r = m.get("roles", {}).get(role)
        if not r or (r["status"] == "pending" and not r["sessions"]):
            continue
        label = "rounds" if role == "verify" else "iterations"
        its = f"{m.get('verify_rounds', 0)} {label}" if role == "verify" else str(r["iterations"])
        rows.append(
            f"| {ms} {m.get('title','')} | {r['status']} (ms: {m.get('status')}) | {r['started_at'] or ''} | {r['ended_at'] or ''} "
            f"| {m.get('trial',0)}{' + upgrade' if m.get('upgraded') else ''} | {its} | {', '.join(r['models'])} "
            f"| {fmt_tokens(r['tokens_in'])} / {fmt_tokens(r['tokens_out'])} | {fmt_tokens(r['cache_read'])} / {fmt_tokens(r['cache_write'])} "
            f"| {r['cost_usd']:.4f} | {' '.join(s[:8] for s in r['sessions'])} |"
        )
    table = "\n".join(rows)
    block = (f"{PROG_START}\n_Generated by the runner (`scripts/loopctl.py`) - do not edit. "
             f"Full session ids: `run-artifacts/sessions.csv`. Updated {now()}._\n\n{table}\n{PROG_END}")
    if path.exists():
        text = path.read_text()
        if PROG_START in text and PROG_END in text:
            text = text[: text.index(PROG_START)] + block + text[text.index(PROG_END) + len(PROG_END):]
        else:
            text = block + "\n\n" + text
    else:
        text = f"# Progress - {role}\n\n## Runner table (per milestone)\n\n{block}\n\n## Agent action log\n\n"
    path.write_text(text)


# ----------------------------------------------------------------------------- task.md
def write_task(data: dict, role: str, ms: str, ref: str | None, base_ref: str | None) -> None:
    m = ms_entry(data, ms)
    r = m["roles"][role]
    spec = m.get("spec") or data.get("spec")
    mf = milestone_file(role, ms) if role != "verify" else None
    lines = [
        f"# Task - {role}", "",
        "_Written by the runner before each iteration. Read it, do not edit it._", "",
        f"- **Spec folder**: `{spec}`",
        f"- **Contract**: `{resolve_contract(spec)}`",
        f"- **Acceptance**: `{spec}/acceptance.md`",
        f"- **Milestone**: {ms} - {m.get('title','')}",
        f"- **Trial**: {m.get('trial',0)} of {CFG['MAX_TRIALS']} failed so far{' (UPGRADE RETRY on ' + CFG['MODEL_UPGRADE'] + ' - last chance before blocked)' if m.get('upgraded') else ''}",
        f"- **Model**: {model_for(m, role)}",
        f"- **Budget left for this milestone**: ${budget_left(m, ms):.2f}",
        f"- **Ref filter**: {ref or 'none (all tasks of the milestone)'}",
    ]
    if role == "verify":
        rnd = m.get("verify_rounds", 0)
        lines += [
            f"- **Round**: {rnd}",
            f"- **Diff base**: `{base_ref}` (review `git diff {base_ref}` plus untracked files)",
            f"- **Report**: `loops/verify/outputs/{ms}-round-{rnd}.md`",
            f"- **Evidence folder**: `loops/verify/outputs/evidence/{ms}/round-{rnd}/`",
            f"- **Loop milestone files**: `{milestone_file('backend-dev', ms)}`, `{milestone_file('frontend-dev', ms)}`",
            "- **Stack**: backend http://localhost:8090 (fresh `stable` db), frontend http://localhost:4300 -> :8090, already running.",
            "- Write `loops/verify/state/verdict.json` last.",
        ]
    else:
        open_, total = count_tasks(role, ms, ref)
        lines += [
            f"- **Iteration**: {r['iterations']} (this attempt {r['attempt_iterations']} of max {r['attempt_cap']})",
            f"- **Your milestone file**: `{mf.relative_to(ROOT) if mf else 'MISSING'}` ({open_} open of {total})",
            f"- **Evidence folder**: `loops/{role}/outputs/evidence/{ms}/`",
            "- Write `loops/{0}/state/result.json` last.".format(role),
        ]
        if r.get("last_result"):
            lines += ["", "## Last iteration", "```json", json.dumps(r["last_result"], indent=2), "```"]
        gate = ROOT / "loops" / role / "state" / "gate.log"
        if gate.exists() and r.get("last_gate") == "fail":
            lines += ["", "## Runner gate FAILED after your last `built` claim (tail)", "```",
                      *gate.read_text().splitlines()[-40:], "```"]
    (ROOT / "loops" / role / "task.md").write_text("\n".join(lines) + "\n")


# ----------------------------------------------------------------------------- failure logic
def fail_trial(m: dict, ms: str, role: str, reason: str) -> str:
    """Count one failed trial for the milestone. Returns 'continue', 'upgrade' or 'blocked'."""
    if m.get("upgraded"):
        m["status"] = "blocked"
        m["blocked_reason"] = f"upgrade retry failed: {reason}"
        event(m, "blocked", role, m["blocked_reason"])
        return "blocked"
    m["trial"] = m.get("trial", 0) + 1
    event(m, "trial", role, f"trial {m['trial']}: {reason}")
    if m["trial"] >= int(CFG["MAX_TRIALS"]):
        m["upgraded"] = True
        m["upgraded_at"] = now()
        event(m, "upgrade", role, f"{CFG['MAX_TRIALS']} trials used - one retry on {CFG['MODEL_UPGRADE']} with ${CFG['BUDGET_UPGRADE_USD']}")
        for r in DEV_ROLES:
            m["roles"][r]["attempt_iterations"] = 0
            m["roles"][r]["attempt_cap"] = None
        return "upgrade"
    return "continue"


# ----------------------------------------------------------------------------- commands
def resolve_contract(spec: str) -> str:
    """Path of the OpenAPI contract of a spec. A spec without its own contracts/openapi.yaml may name another
    one in contracts/contract-source.txt (used by frontend-only enhancement specs)."""
    own = ROOT / spec / "contracts" / "openapi.yaml"
    if own.exists():
        return f"{spec}/contracts/openapi.yaml"
    src = ROOT / spec / "contracts" / "contract-source.txt"
    if src.exists():
        return src.read_text().strip()
    sys.exit(f"loopctl: no contract for {spec} (neither contracts/openapi.yaml nor contracts/contract-source.txt)")


def cmd_get_contract(a):
    data = load()
    spec = data.get("spec")
    if a.ms:
        spec = ms_entry(data, a.ms).get("spec") or spec
    print(resolve_contract(spec))


def cmd_get_spec(a):
    data = load()
    if not data.get("spec"):
        sys.exit("loopctl: no spec recorded in milestones.json (run split-tasks.py)")
    print(data["spec"])


def cmd_ms_list(a):
    data = load()
    for ms in data.get("order", []):
        m = data["milestones"][ms]
        if a.pending and m.get("status") == "passed":
            continue
        print(f"{ms}\t{m.get('status')}\t{m.get('title','')}")


def cmd_get(a):
    data = load()
    m = ms_entry(data, a.ms)
    if a.role:
        r = m["roles"][a.role]
        val = {"model": model_for(m, a.role), "budget_left": budget_left(m, a.ms)}.get(a.field, r.get(a.field, m.get(a.field)))
    else:
        val = {"budget_left": budget_left(m, a.ms)}.get(a.field, m.get(a.field))
    print("" if val is None else (json.dumps(val) if isinstance(val, (dict, list)) else val))


def cmd_set_status(a):
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        if a.role:
            m["roles"][a.role]["status"] = a.status
            if a.status in ("built", "blocked", "passed") and not m["roles"][a.role]["ended_at"]:
                m["roles"][a.role]["ended_at"] = now()
        else:
            m["status"] = a.status
            if a.reason:
                m["blocked_reason"] = a.reason
                event(m, a.status, "runner", a.reason)
            if a.status == "passed":
                m["ended_at"] = now()
                m["tag"] = a.tag
        save(data)
        for r in ROLES:
            write_role_state(data, r, a.ms)
            render_progress(data, r)


def cmd_preflight(a):
    """Prepare one dev iteration. Prints 'run <iteration> <model> <max_budget>' or 'stop <reason>'."""
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        r = m["roles"][a.role]
        if m.get("status") in ("passed", "blocked"):
            print(f"stop milestone-{m['status']}"); return
        if r["status"] == "built":
            print("stop built"); return
        open_, total = count_tasks(a.role, a.ms, a.ref)
        if total == 0:
            r["status"] = "built"; r["ended_at"] = r["ended_at"] or now()
            event(m, "info", a.role, "no tasks for this role in this milestone")
            save(data); write_role_state(data, a.role, a.ms); render_progress(data, a.role)
            print("stop no-tasks"); return
        left = budget_left(m, a.ms)
        if left <= 0.05:
            m["status"] = "blocked"; m["blocked_reason"] = f"budget exhausted (${left:.2f} left)"
            event(m, "blocked", a.role, m["blocked_reason"])
            save(data); write_role_state(data, a.role, a.ms); render_progress(data, a.role)
            print("stop blocked-budget"); return
        if r["attempt_cap"] is None:
            r["attempt_cap"] = attempt_cap(open_)
            r["attempt_iterations"] = 0
        if r["attempt_iterations"] >= r["attempt_cap"]:
            verdict = fail_trial(m, a.ms, a.role, f"iteration cap {r['attempt_cap']} reached with {open_} open tasks")
            r["attempt_iterations"] = 0
            r["attempt_cap"] = attempt_cap(open_)
            if verdict == "blocked":
                save(data); write_role_state(data, a.role, a.ms); render_progress(data, a.role)
                print("stop blocked-iteration-cap"); return
        r["iterations"] += 1
        r["attempt_iterations"] += 1
        r["status"] = "building"
        r["started_at"] = r["started_at"] or now()
        m["started_at"] = m.get("started_at") or now()
        if m.get("status") in (None, "pending", "failed"):
            m["status"] = "building"
        save(data)
        write_task(data, a.role, a.ms, a.ref, None)
        write_role_state(data, a.role, a.ms)
        render_progress(data, a.role)
        print(f"run {r['iterations']} {model_for(m, a.role)} {left:.2f} {m.get('trial', 0)}")


def cmd_verify_start(a):
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        left = budget_left(m, a.ms)
        if left <= 0.05:
            m["status"] = "blocked"; m["blocked_reason"] = f"budget exhausted before verify (${left:.2f} left)"
            event(m, "blocked", "verify", m["blocked_reason"])
            save(data); write_role_state(data, "verify", a.ms); render_progress(data, "verify")
            print("stop blocked-budget"); return
        m["verify_rounds"] = m.get("verify_rounds", 0) + 1
        m["status"] = "verifying"
        r = m["roles"]["verify"]
        r["status"] = "verifying"; r["started_at"] = r["started_at"] or now(); r["iterations"] += 1
        save(data)
        write_task(data, "verify", a.ms, None, a.base_ref)
        write_role_state(data, "verify", a.ms)
        render_progress(data, "verify")
        print(f"run {m['verify_rounds']} {model_for(m, 'verify')} {left:.2f} {m.get('trial', 0)}")


def parse_session(path: Path) -> dict:
    try:
        raw = json.loads(path.read_text() or "{}")
    except json.JSONDecodeError:
        raw = {}
    if isinstance(raw, list):  # stream-json style fallback: take the result event
        raw = next((e for e in reversed(raw) if e.get("type") == "result"), {})
    u = raw.get("usage") or {}
    model_usage = raw.get("modelUsage") or {}
    return {
        "session_id": raw.get("session_id", ""),
        "cost_usd": float(raw.get("total_cost_usd") or 0.0),
        "input_tokens": int(u.get("input_tokens") or 0),
        "output_tokens": int(u.get("output_tokens") or 0),
        "cache_read_tokens": int(u.get("cache_read_input_tokens") or 0),
        "cache_write_tokens": int(u.get("cache_creation_input_tokens") or 0),
        "duration_ms": int(raw.get("duration_ms") or 0),
        "num_turns": int(raw.get("num_turns") or 0),
        "is_error": bool(raw.get("is_error", not raw)),
        "subtype": raw.get("subtype", "missing-output"),
        "models": sorted(model_usage.keys()),
    }


def append_csv(path: Path, fields: list[str], row: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    new = not path.exists() or path.stat().st_size == 0
    with open(path, "a", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=fields)
        if new:
            w.writeheader()
        w.writerow({k: row.get(k, "") for k in fields})


def cmd_record(a):
    s = parse_session(Path(a.json))
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        r = m["roles"][a.role]
        for k_src, k_dst in (("input_tokens", "tokens_in"), ("output_tokens", "tokens_out"),
                             ("cache_read_tokens", "cache_read"), ("cache_write_tokens", "cache_write")):
            r[k_dst] += s[k_src]
        r["cost_usd"] = round(r["cost_usd"] + s["cost_usd"], 6)
        if m.get("upgraded"):
            m["upgrade_cost_usd"] = round(m.get("upgrade_cost_usd", 0.0) + s["cost_usd"], 6)
        m["cost_usd"] = round(m.get("cost_usd", 0.0) + s["cost_usd"], 6)
        if s["session_id"]:
            r["sessions"].append(s["session_id"])
        model = a.model
        if model not in r["models"]:
            r["models"].append(model)
        save(data)
        append_csv(SESSIONS_CSV, SESSION_FIELDS, {
            "timestamp": now(), "spec": data.get("spec"), "milestone": a.ms, "role": a.role,
            "trial": m.get("trial", 0), "iteration": a.iteration, "round": a.round or "",
            "model": model, "session_id": s["session_id"], "prompt_file": a.prompt_file,
            "input_tokens": s["input_tokens"], "output_tokens": s["output_tokens"],
            "cache_read_tokens": s["cache_read_tokens"], "cache_write_tokens": s["cache_write_tokens"],
            "cost_usd": f"{s['cost_usd']:.6f}", "duration_ms": s["duration_ms"], "num_turns": s["num_turns"],
            "is_error": s["is_error"], "result": s["subtype"], "log_file": a.json,
        })
        write_role_state(data, a.role, a.ms)
        render_progress(data, a.role)
    print(f"recorded {a.role} {a.ms} session={s['session_id'][:8]} cost=${s['cost_usd']:.4f} error={s['is_error']}")


def cmd_decide(a):
    """After a dev iteration: read state/result.json + gate outcome. Prints continue|built|upgrade|blocked."""
    res_path = ROOT / "loops" / a.role / "state" / "result.json"
    try:
        res = json.loads(res_path.read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        res = {"result": "missing", "tasks_done": [], "notes": "no valid state/result.json (session crashed, timed out or ignored the protocol)"}
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        r = m["roles"][a.role]
        r["last_result"] = res
        r["last_gate"] = a.gate
        open_, _ = count_tasks(a.role, a.ms, a.ref)
        outcome = "continue"
        kind = res.get("result")
        if m.get("status") == "blocked":
            outcome = "blocked"
        elif kind == "built" and a.gate == "pass" and open_ == 0:
            r["status"] = "built"; r["ended_at"] = now()
            outcome = "built"
        elif kind == "built" and open_ > 0:
            outcome = fail_trial(m, a.ms, a.role, f"claimed built with {open_} open tasks")
        elif kind == "built":
            outcome = fail_trial(m, a.ms, a.role, "claimed built but runner gate failed")
        elif kind in ("selfcheck_failed", "stuck", "missing"):
            outcome = fail_trial(m, a.ms, a.role, f"{kind}: {res.get('notes', '')[:200]}")
        elif kind == "progress" and not res.get("tasks_done"):
            outcome = fail_trial(m, a.ms, a.role, "no progress in iteration")
        if outcome == "blocked":
            r["status"] = "blocked"; r["ended_at"] = now()
        save(data)
        write_role_state(data, a.role, a.ms)
        render_progress(data, a.role)
    print(outcome)


def next_fix_id(text: str) -> int:
    ids = [int(x) for x in re.findall(r"FIX-(\d+)", text)]
    return max(ids, default=0) + 1


def add_fix_tasks(role: str, ms: str, tasks: list[dict], rnd: int) -> int:
    f = milestone_file(role, ms)
    if not f or not tasks:
        return 0
    text = f.read_text()
    n = next_fix_id(text)
    new_lines = []
    for t in tasks:
        ev = f" (evidence: {t['evidence']})" if t.get("evidence") else ""
        new_lines.append(f"- [ ] FIX-{n:02d} [verify round {rnd}] {t['text']}{ev}")
        n += 1
    header = "## FIX tasks"
    if header in text:
        idx = text.index(header) + len(header)
        text = text[:idx] + "\n" + "\n".join(new_lines) + text[idx:]
    else:
        parts = text.split("\n## ", 1)
        insert = f"{header}\n" + "\n".join(new_lines) + "\n\n"
        text = parts[0].rstrip() + "\n\n" + insert + ("## " + parts[1] if len(parts) > 1 else "")
    f.write_text(text)
    return len(new_lines)


def cmd_verdict(a):
    """Apply loops/verify/state/verdict.json. Prints pass|reject|upgrade|blocked|error."""
    vpath = Path(a.file)
    try:
        v = json.loads(vpath.read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        v = None
    with locked():
        data = load()
        m = ms_entry(data, a.ms)
        rnd = m.get("verify_rounds", 0)
        vr = m["roles"]["verify"]
        if not v or v.get("verdict") not in ("PASS", "REJECT"):
            m.setdefault("verify_errors", 0)
            m["verify_errors"] += 1
            event(m, "verify-error", "verify", f"round {rnd}: no valid verdict.json")
            outcome = "error"
            if m["verify_errors"] >= 2:
                m["status"] = "blocked"; m["blocked_reason"] = "verifier produced no valid verdict twice"
                outcome = "blocked"
        elif v.get("escalate"):
            m["status"] = "blocked"; m["blocked_reason"] = f"verifier escalated: {v.get('notes', '')[:300]}"
            event(m, "blocked", "verify", m["blocked_reason"])
            outcome = "blocked"
        elif v["verdict"] == "PASS":
            m["status"] = "verified"
            vr["status"] = "passed"; vr["ended_at"] = now()
            event(m, "verdict", "verify", f"round {rnd}: PASS ({len(v.get('fixes', []))} small fixes)")
            outcome = "pass"
        else:
            event(m, "verdict", "verify", f"round {rnd}: REJECT ({len(v.get('fix_tasks', []))} FIX tasks)")
            outcome = fail_trial(m, a.ms, "verify", f"verifier REJECT round {rnd}")
            outcome = "reject" if outcome == "continue" else outcome
            if outcome != "blocked":
                by_role: dict[str, list] = {}
                for t in v.get("fix_tasks", []):
                    by_role.setdefault(t.get("role", "backend-dev"), []).append(t)
                if not by_role:
                    by_role = {"backend-dev": [{"text": "Verifier rejected without FIX tasks - see report " + f"loops/verify/outputs/{a.ms}-round-{rnd}.md"}]}
                for role, tasks in by_role.items():
                    if role not in DEV_ROLES:
                        continue
                    add_fix_tasks(role, a.ms, tasks, rnd)
                    rr = m["roles"][role]
                    rr["status"] = "building"; rr["ended_at"] = None
                    rr["attempt_iterations"] = 0; rr["attempt_cap"] = None
                m["status"] = "building"
            vr["status"] = "rejected" if outcome != "blocked" else "blocked"
        save(data)
        for r in ROLES:
            write_role_state(data, r, a.ms)
            render_progress(data, r)
    print(outcome)


def cmd_roles_to_run(a):
    data = load()
    m = ms_entry(data, a.ms)
    print(" ".join(r for r in DEV_ROLES if m["roles"][r]["status"] not in ("built",)))


# ----------------------------------------------------------------------------- swagger diff
def _yaml_load(p: Path):
    import yaml  # PyYAML
    return yaml.safe_load(p.read_text())


def _ref_name(schema: dict | None) -> str | None:
    if not isinstance(schema, dict):
        return None
    if "$ref" in schema:
        return schema["$ref"].rsplit("/", 1)[-1]
    if schema.get("type") == "array" and isinstance(schema.get("items"), dict):
        n = _ref_name(schema["items"])
        return f"{n}[]" if n else None
    return None


def _norm_path(p: str) -> str:
    return re.sub(r"\{[^}]+\}", "{}", p.rstrip("/") or "/")


def cmd_swagger_diff(a):
    contract = _yaml_load(Path(a.contract))
    exported = json.loads(Path(a.exported).read_text())
    problems, warnings, checked = [], [], 0
    exp_ops = {}
    for p, ops in (exported.get("paths") or {}).items():
        for meth, op in ops.items():
            if meth.lower() in ("get", "post", "put", "patch", "delete"):
                exp_ops[(meth.lower(), _norm_path(p))] = (p, op)
    in_scope_schemas = set()
    contract_keys = set()
    for p, ops in (contract.get("paths") or {}).items():
        for meth, op in ops.items():
            if meth.lower() not in ("get", "post", "put", "patch", "delete"):
                continue
            key = (meth.lower(), _norm_path(p))
            contract_keys.add(key)
            ms = op.get("x-milestone") or ops.get("x-milestone")
            if not ms:
                warnings.append(f"`{meth.upper()} {p}` has no x-milestone in the contract (not checked)")
                continue
            if ms > a.ms:
                continue
            checked += 1
            if key not in exp_ops:
                problems.append(f"MISSING `{meth.upper()} {p}` ({ms})")
                continue
            _, eop = exp_ops[key]
            c_codes = {str(c) for c in (op.get("responses") or {})}
            e_codes = {str(c) for c in (eop.get("responses") or {})}
            miss = sorted(c for c in c_codes if c not in e_codes and c != "default")
            if miss:
                problems.append(f"`{meth.upper()} {p}`: response codes {miss} not declared in code (declared: {sorted(e_codes)})")
            c_params = {(x.get("in"), x.get("name")) for x in op.get("parameters", []) if isinstance(x, dict) and "name" in x}
            e_params = {(x.get("in"), x.get("name")) for x in eop.get("parameters", []) if isinstance(x, dict)}
            if c_params - e_params:
                problems.append(f"`{meth.upper()} {p}`: parameters missing in code {sorted(c_params - e_params)}")
            c_body = _ref_name(((op.get("requestBody") or {}).get("content") or {}).get("application/json", {}).get("schema"))
            e_body = _ref_name(((eop.get("requestBody") or {}).get("content") or {}).get("application/json", {}).get("schema"))
            if c_body and c_body != e_body:
                problems.append(f"`{meth.upper()} {p}`: request body schema `{c_body}` in contract, `{e_body}` in code")
            if c_body:
                in_scope_schemas.add(c_body.rstrip("[]"))
            for code, resp in (op.get("responses") or {}).items():
                n = _ref_name(((resp or {}).get("content") or {}).get("application/json", {}).get("schema"))
                if n:
                    in_scope_schemas.add(n.rstrip("[]"))
    for key, (p, _) in exp_ops.items():
        if key not in contract_keys:
            problems.append(f"EXTRA `{key[0].upper()} {p}` exists in code but not in the contract")
    c_schemas = (contract.get("components") or {}).get("schemas") or {}
    e_schemas = (exported.get("components") or {}).get("schemas") or {}
    for name in sorted(in_scope_schemas):
        cs, es = c_schemas.get(name), e_schemas.get(name)
        if cs is None:
            continue
        if es is None:
            problems.append(f"schema `{name}` not found in exported Swagger (DTO class name must equal the contract schema name)")
            continue
        cp, ep = set((cs.get("properties") or {})), set((es.get("properties") or {}))
        if cp - ep:
            problems.append(f"schema `{name}`: properties missing in code {sorted(cp - ep)}")
        if ep - cp:
            problems.append(f"schema `{name}`: extra properties in code {sorted(ep - cp)}")
        for prop in sorted(cp & ep):
            ce, ee = (cs["properties"][prop] or {}).get("enum"), (es["properties"][prop] or {}).get("enum")
            if ce and set(map(str, ce)) != set(map(str, ee or [])):
                problems.append(f"schema `{name}.{prop}`: enum {ce} in contract, {ee} in code")
    out = [f"# Swagger diff - up to {a.ms}", "", f"- Contract: `{a.contract}`", f"- Exported: `{a.exported}`",
           f"- Operations checked: {checked}", f"- Result: **{'PASS' if not problems else 'FAIL'}**", ""]
    out += ["## Problems", *(f"- {x}" for x in problems or ["none"]), "", "## Warnings", *(f"- {x}" for x in warnings or ["none"])]
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    Path(a.out).write_text("\n".join(out) + "\n")
    print("\n".join(out[5:6] + [f"- {x}" for x in problems[:30]]))
    sys.exit(1 if problems else 0)


# ----------------------------------------------------------------------------- reporting
def read_csv(path: Path) -> list[dict]:
    if not path.exists():
        return []
    with open(path, newline="") as fh:
        return list(csv.DictReader(fh))


def cmd_summary(a):
    data = load()
    rows = read_csv(SESSIONS_CSV)
    lines = ["# Run summary", "", f"_Generated {now()} from `loops/_shared/milestones.json` and `run-artifacts/sessions.csv`._", "",
             "| Milestone | Status | Start | End | Trials | Upgraded | Verify rounds | BE it. | FE it. | Sessions | Cost USD | Tag |",
             "|---|---|---|---|---|---|---|---|---|---|---|---|"]
    total = 0.0
    for ms in data.get("order", []):
        m = data["milestones"][ms]
        roles = m.get("roles", {})
        n_sess = sum(len(roles.get(r, {}).get("sessions", [])) for r in ROLES)
        total += m.get("cost_usd", 0.0)
        lines.append(f"| {ms} {m.get('title','')} | {m.get('status')} | {m.get('started_at') or ''} | {m.get('ended_at') or ''} | {m.get('trial',0)} "
                     f"| {'yes' if m.get('upgraded') else 'no'} | {m.get('verify_rounds',0)} | {roles.get('backend-dev',{}).get('iterations',0)} "
                     f"| {roles.get('frontend-dev',{}).get('iterations',0)} | {n_sess} | {m.get('cost_usd',0.0):.4f} | {m.get('tag') or ''} |")
    lines += ["", f"**Total cost (headless loops)**: ${total:.4f} over {len(rows)} sessions.", ""]
    by_model: dict[str, list] = {}
    for r in rows:
        by_model.setdefault(r["model"], []).append(r)
    lines += ["| Model | Sessions | Input | Output | Cache read | Cache write | Cost USD |", "|---|---|---|---|---|---|---|"]
    for model, rs in sorted(by_model.items()):
        s = lambda k: sum(int(x[k] or 0) for x in rs)
        lines.append(f"| {model} | {len(rs)} | {fmt_tokens(s('input_tokens'))} | {fmt_tokens(s('output_tokens'))} | {fmt_tokens(s('cache_read_tokens'))} "
                     f"| {fmt_tokens(s('cache_write_tokens'))} | {sum(float(x['cost_usd'] or 0) for x in rs):.4f} |")
    lines += ["", "## Events"]
    for ms in data.get("order", []):
        for e in data["milestones"][ms].get("events", []):
            lines.append(f"- {e['at']} · {ms} · {e['role']} · {e['type']} · {e['detail']}")
    out = ROOT / "run-artifacts" / "summary.md"
    out.write_text("\n".join(lines) + "\n")
    print(out.relative_to(ROOT))


def cmd_log_manual(a):
    prompt = Path(a.prompt_file).read_text() if a.prompt_file else a.prompt
    append_csv(MANUAL_CSV, MANUAL_FIELDS, {"timestamp": a.timestamp or now(), "phase": a.phase, "session_id": a.session_id,
                                          "model": a.model, "prompt": prompt, "notes": a.notes or ""})
    print("logged")


def cmd_import_transcript(a):
    """Import the user prompts of an interactive Claude Code session (.jsonl) into manual-prompts.csv."""
    n = 0
    sid = Path(a.jsonl).stem
    with open(a.jsonl) as fh:
        for line in fh:
            try:
                e = json.loads(line)
            except json.JSONDecodeError:
                continue
            if e.get("type") != "user" or e.get("isMeta") or e.get("isSidechain"):
                continue
            msg = e.get("message") or {}
            content = msg.get("content")
            if isinstance(content, list):
                texts = [c.get("text", "") for c in content if isinstance(c, dict) and c.get("type") == "text"]
                if not texts:
                    continue  # tool results
                content = "\n".join(texts)
            if not isinstance(content, str) or not content.strip() or content.startswith("<command-") and "<command-args>" not in content:
                continue
            append_csv(MANUAL_CSV, MANUAL_FIELDS, {"timestamp": e.get("timestamp", ""), "phase": a.phase,
                                                  "session_id": e.get("sessionId", sid), "model": a.model, "prompt": content.strip(), "notes": a.notes or ""})
            n += 1
    print(f"imported {n} prompts from session {sid}")


def cmd_excel(a):
    from openpyxl import Workbook
    from openpyxl.styles import Alignment, Font, PatternFill
    from openpyxl.utils import get_column_letter

    wb = Workbook()
    head_font, head_fill = Font(bold=True, color="FFFFFF"), PatternFill("solid", fgColor="305496")

    def sheet(ws, header, rows, widths):
        ws.append(header)
        for c in ws[1]:
            c.font, c.fill = head_font, head_fill
        for r in rows:
            ws.append(r)
        for i, w in enumerate(widths, 1):
            ws.column_dimensions[get_column_letter(i)].width = w
        for row in ws.iter_rows(min_row=2):
            for c in row:
                c.alignment = Alignment(wrap_text=True, vertical="top")
        ws.freeze_panes = "A2"
        ws.auto_filter.ref = ws.dimensions

    manual = read_csv(MANUAL_CSV)
    sessions = read_csv(SESSIONS_CSV)
    prompt_rows = [[m["timestamp"], "interactive", m["phase"], "", "", m["model"], m["session_id"], m["prompt"]] for m in manual]
    for s in sessions:
        pf = ROOT / s["prompt_file"] if s["prompt_file"] else None
        text = pf.read_text() if pf and pf.exists() else ""
        prompt_rows.append([s["timestamp"], "headless", s["role"], s["milestone"], s["iteration"] or s["round"], s["model"], s["session_id"], text])
    ws = wb.active
    ws.title = "Prompts"
    sheet(ws, ["Timestamp", "Kind", "Phase / role", "Milestone", "Iteration / round", "Model", "Session id", "Prompt"],
          prompt_rows, [24, 12, 16, 10, 10, 20, 38, 100])
    sheet(wb.create_sheet("Sessions"), SESSION_FIELDS, [[s.get(k, "") for k in SESSION_FIELDS] for s in sessions],
          [24, 22, 10, 14, 6, 9, 6, 20, 38, 40, 12, 12, 12, 12, 10, 12, 8, 8, 14, 50])
    data = load()
    ms_rows = []
    for ms in data.get("order", []):
        m = data["milestones"][ms]
        ro = m.get("roles", {})
        ms_rows.append([ms, m.get("title", ""), m.get("status"), m.get("started_at"), m.get("ended_at"), m.get("trial", 0),
                        "yes" if m.get("upgraded") else "no", m.get("verify_rounds", 0),
                        ro.get("backend-dev", {}).get("iterations", 0), ro.get("frontend-dev", {}).get("iterations", 0),
                        round(m.get("cost_usd", 0.0), 4), m.get("tag") or "", m.get("blocked_reason") or ""])
    sheet(wb.create_sheet("Milestones"), ["Milestone", "Title", "Status", "Start", "End", "Trials", "Upgraded", "Verify rounds",
                                          "BE iterations", "FE iterations", "Cost USD", "Tag", "Blocked reason"],
          ms_rows, [10, 30, 10, 24, 24, 7, 9, 8, 8, 8, 10, 8, 40])
    out = ROOT / (a.out or "run-artifacts/QuickFlow-prompts-sessions.xlsx")
    wb.save(out)
    print(out.relative_to(ROOT))


def cmd_status(a):
    data = load()
    print(f"spec: {data.get('spec')}")
    for ms in data.get("order", []):
        m = data["milestones"][ms]
        ro = m.get("roles", {})
        rs = " ".join(f"{r.split('-')[0]}={ro.get(r, {}).get('status', '-')}" for r in ROLES)
        print(f"{ms} {m.get('status'):<10} trial={m.get('trial',0)}{'+up' if m.get('upgraded') else ''} cost=${m.get('cost_usd',0):.3f} "
              f"left=${budget_left(m, ms):.2f} {rs}  {m.get('title','')}")
        if m.get("blocked_reason"):
            print(f"    blocked: {m['blocked_reason']}")


def cmd_render(a):
    data = load()
    for r in ROLES:
        render_progress(data, r)
        if a.ms:
            write_role_state(data, r, a.ms)
    print("rendered")


# ----------------------------------------------------------------------------- main
def main():
    p = argparse.ArgumentParser(prog="loopctl.py", description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = p.add_subparsers(dest="cmd", required=True)

    sp.add_parser("get-spec", help="print the active spec folder").set_defaults(fn=cmd_get_spec)
    x = sp.add_parser("get-contract", help="print the OpenAPI contract path of a milestone's spec (or the active spec)"); x.add_argument("--ms"); x.set_defaults(fn=cmd_get_contract)
    sp.add_parser("status", help="print milestone status").set_defaults(fn=cmd_status)
    x = sp.add_parser("ms-list", help="milestones in order (tab separated)"); x.add_argument("--pending", action="store_true"); x.set_defaults(fn=cmd_ms_list)
    x = sp.add_parser("get", help="print one field"); x.add_argument("--ms", required=True); x.add_argument("--role"); x.add_argument("field"); x.set_defaults(fn=cmd_get)
    x = sp.add_parser("set-status"); x.add_argument("--ms", required=True); x.add_argument("--role"); x.add_argument("--status", required=True)
    x.add_argument("--reason"); x.add_argument("--tag"); x.set_defaults(fn=cmd_set_status)
    x = sp.add_parser("preflight", help="prepare a dev iteration"); x.add_argument("--role", required=True, choices=DEV_ROLES); x.add_argument("--ms", required=True)
    x.add_argument("--ref"); x.set_defaults(fn=cmd_preflight)
    x = sp.add_parser("verify-start", help="prepare a verify round"); x.add_argument("--ms", required=True); x.add_argument("--base-ref", required=True); x.set_defaults(fn=cmd_verify_start)
    x = sp.add_parser("record", help="record a session JSON"); x.add_argument("--role", required=True, choices=ROLES); x.add_argument("--ms", required=True)
    x.add_argument("--json", required=True); x.add_argument("--prompt-file", required=True); x.add_argument("--model", required=True)
    x.add_argument("--iteration", default=""); x.add_argument("--round", default=""); x.set_defaults(fn=cmd_record)
    x = sp.add_parser("decide", help="decide after a dev iteration"); x.add_argument("--role", required=True, choices=DEV_ROLES); x.add_argument("--ms", required=True)
    x.add_argument("--gate", required=True, choices=("pass", "fail", "skip")); x.add_argument("--ref"); x.set_defaults(fn=cmd_decide)
    x = sp.add_parser("verdict", help="apply verify/state/verdict.json"); x.add_argument("--ms", required=True)
    x.add_argument("--file", default=str(ROOT / "loops/verify/state/verdict.json")); x.set_defaults(fn=cmd_verdict)
    x = sp.add_parser("roles-to-run"); x.add_argument("--ms", required=True); x.set_defaults(fn=cmd_roles_to_run)
    x = sp.add_parser("swagger-diff"); x.add_argument("--contract", required=True); x.add_argument("--exported", required=True)
    x.add_argument("--ms", required=True); x.add_argument("--out", required=True); x.set_defaults(fn=cmd_swagger_diff)
    sp.add_parser("summary", help="write run-artifacts/summary.md").set_defaults(fn=cmd_summary)
    x = sp.add_parser("excel", help="write the prompts/sessions Excel file"); x.add_argument("--out"); x.set_defaults(fn=cmd_excel)
    x = sp.add_parser("log-manual", help="append an interactive prompt"); x.add_argument("--phase", required=True); x.add_argument("--session-id", required=True)
    x.add_argument("--model", default=CFG.get("MODEL_PLAN")); x.add_argument("--prompt"); x.add_argument("--prompt-file"); x.add_argument("--notes"); x.add_argument("--timestamp")
    x.set_defaults(fn=cmd_log_manual)
    x = sp.add_parser("import-transcript", help="import user prompts of an interactive session .jsonl"); x.add_argument("--jsonl", required=True)
    x.add_argument("--phase", required=True); x.add_argument("--model", default=CFG.get("MODEL_PLAN")); x.add_argument("--notes"); x.set_defaults(fn=cmd_import_transcript)
    x = sp.add_parser("render", help="re-render progress.md tables"); x.add_argument("--ms"); x.set_defaults(fn=cmd_render)

    a = p.parse_args()
    a.fn(a)


if __name__ == "__main__":
    main()
