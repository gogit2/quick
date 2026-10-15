# Contract for spec 002

**No new contract.** Spec 002 is frontend-only and reuses the API contract of spec 001 **unchanged**:
[`../../001-quickflow/contracts/openapi.yaml`](../../001-quickflow/contracts/openapi.yaml) (35 operations).

- `scripts/mock.sh start` and the loops read the contract from the spec folder recorded in `loops/_shared/milestones.json`. For M08-M11 the runner passes `specs/002-visual-polish`; `loopctl` is told that this spec's contract lives in spec 001 (see plan.md, "Runner note").
- The UI contract of this spec is not an API: it is the set of existing **`data-testid`s** (unchanged) and the **design tokens** in [`../data-model.md`](../data-model.md).
