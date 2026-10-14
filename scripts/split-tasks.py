#!/usr/bin/env python3
"""split-tasks: turn specs/<NNN>/tasks.md into per-loop milestone files.

Expected tasks.md shape (speckit-tasks is told to produce it):

    ## Milestone M02: Tasks
    **Goal**: ...
    - [ ] T010 [BE] [US-TSK-01] Create Flyway migration V2__tasks.sql ...
    - [ ] T011 [P] [FE] [US-TSK-01] Tasks page list with data-testid ...

Writes   loops/backend-dev/outputs/Mxx-<slug>.md   ([BE] tasks)
         loops/frontend-dev/outputs/Mxx-<slug>.md  ([FE] tasks)
and registers the milestones in loops/_shared/milestones.json.

Re-running is safe: ticks, notes and FIX tasks already in a milestone file are kept.

Usage: split-tasks.py [--spec specs/001-quickflow] [--dry-run]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scripts"))
import importlib.util  # noqa: E402

_spec = importlib.util.spec_from_file_location("loopctl", ROOT / "scripts" / "loopctl.py")
loopctl = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(loopctl)

MS_RE = re.compile(r"^##\s+(?:Phase\s+\d+\s*[:\-—]\s*)?(?:Milestone\s+)?(M\d{2})\b\s*[:\-—]?\s*(.*)$")
TASK_RE = re.compile(r"^\s*- \[( |x|X)\] (T\d+)\b(.*)$")
ROLE_TAG = {"[BE]": "backend-dev", "[FE]": "frontend-dev"}


def slugify(s: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")
    return s[:40].strip("-") or "milestone"


def default_spec() -> str:
    fj = ROOT / ".specify" / "feature.json"
    if fj.exists():
        d = json.loads(fj.read_text()).get("feature_directory")
        if d:
            return str(Path(d).resolve().relative_to(ROOT)) if Path(d).is_absolute() else d
    specs = sorted(p for p in (ROOT / "specs").glob("[0-9][0-9][0-9]-*") if p.is_dir())
    if not specs:
        sys.exit("split-tasks: no spec folder found")
    return str(specs[-1].relative_to(ROOT))


def parse(tasks_md: Path):
    milestones, cur = [], None
    for line in tasks_md.read_text().splitlines():
        m = MS_RE.match(line)
        if m:
            cur = {"id": m.group(1), "title": m.group(2).strip() or m.group(1), "goal": "", "tasks": []}
            milestones.append(cur)
            continue
        if cur is None:
            continue
        if line.startswith("**Goal**") and not cur["goal"]:
            cur["goal"] = line.split(":", 1)[-1].strip(" *")
        t = TASK_RE.match(line)
        if t:
            roles = [r for tag, r in ROLE_TAG.items() if tag in line]
            if len(roles) != 1:
                sys.exit(f"split-tasks: task {t.group(2)} in {cur['id']} must have exactly one of [BE] / [FE]: {line.strip()}")
            cur["tasks"].append({"id": t.group(2), "role": roles[0], "line": f"- [ ] {t.group(2)}{t.group(3)}".rstrip()})
    return milestones


def merge_file(path: Path, header: str, tasks: list[dict]) -> str:
    existing_lines, fix_block = {}, ""
    if path.exists():
        text = path.read_text()
        for line in text.splitlines():
            t = TASK_RE.match(line)
            if t:
                existing_lines[t.group(2)] = line
        if "## FIX tasks" in text:
            after = text.split("## FIX tasks", 1)[1]
            fix_block = "## FIX tasks" + after.split("\n## ", 1)[0].rstrip() + "\n\n"
    body = [existing_lines.get(t["id"], t["line"]) for t in tasks]
    return header + fix_block + "## Tasks\n\n" + "\n".join(body) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--spec", help="spec folder, e.g. specs/001-quickflow (default: current Spec Kit feature)")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    spec = a.spec or default_spec()
    tasks_md = ROOT / spec / "tasks.md"
    if not tasks_md.exists():
        sys.exit(f"split-tasks: {tasks_md} not found")
    milestones = parse(tasks_md)
    if not milestones:
        sys.exit("split-tasks: no '## Milestone Mxx: <title>' headings found in tasks.md")

    with loopctl.locked():
        data = loopctl.load()
        for ms in milestones:
            old = data["milestones"].get(ms["id"])
            if old and old.get("spec") and old["spec"] != spec:
                sys.exit(f"split-tasks: {ms['id']} already belongs to {old['spec']} - milestone numbers must continue across specs")
        now = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
        for ms in milestones:
            slug = slugify(ms["title"])
            counts = {}
            for role in ("backend-dev", "frontend-dev"):
                tasks = [t for t in ms["tasks"] if t["role"] == role]
                counts[role] = len(tasks)
                tag = "BE" if role == "backend-dev" else "FE"
                header = (f"# {ms['id']} - {ms['title']} [{tag}] ({role})\n\n"
                          f"- Spec: `{spec}` · source: `{spec}/tasks.md` · split: {now}\n"
                          f"- Goal: {ms['goal'] or '(see tasks.md)'}\n"
                          f"- Acceptance: `{spec}/acceptance.md` section {ms['id']}\n"
                          f"- Tick a task only when done and self-checked: `- [x] T012 ... - done: <note, evidence>`\n\n")
                outdir = ROOT / "loops" / role / "outputs"
                for stale in outdir.glob(f"{ms['id']}-*.md"):
                    if stale.name != f"{ms['id']}-{slug}.md" and not a.dry_run:
                        stale.rename(outdir / f"{ms['id']}-{slug}.md")
                path = outdir / f"{ms['id']}-{slug}.md"
                content = merge_file(path, header, tasks)
                if a.dry_run:
                    print(f"--- {path.relative_to(ROOT)} ({len(tasks)} tasks)")
                else:
                    path.write_text(content)
            entry = data["milestones"].setdefault(ms["id"], {
                "status": "pending", "trial": 0, "upgraded": False, "cost_usd": 0.0, "upgrade_cost_usd": 0.0,
                "verify_rounds": 0, "started_at": None, "ended_at": None, "tag": None, "blocked_reason": None,
            })
            entry.update({"title": ms["title"], "slug": slug, "spec": spec})
            loopctl.ms_entry(data, ms["id"])
            for role, n in counts.items():
                entry["roles"][role]["tasks"] = n
            if ms["id"] not in data["order"]:
                data["order"].append(ms["id"])
            print(f"{ms['id']} {ms['title']}: BE {counts['backend-dev']} · FE {counts['frontend-dev']} tasks")
        data["order"].sort()
        data["spec"] = spec
        if not a.dry_run:
            loopctl.save(data)
            for role in loopctl.ROLES:
                loopctl.render_progress(data, role)
    print(f"spec {spec}: {len(milestones)} milestones{' (dry run)' if a.dry_run else ''}")


if __name__ == "__main__":
    main()
