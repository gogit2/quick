# Quickstart: validating QuickFlow

Prerequisites: Java 17, Node 18.20, `tools/` installed (`cd tools && npm ci`, then Chromium into `tools/.browsers`, see README). Contract: [contracts/openapi.yaml](contracts/openapi.yaml). Data: [data-model.md](data-model.md).

## 1. Backend alone
```bash
scripts/backend.sh start --port 8091 --db selfcheck --fresh
scripts/curl-regression.sh --base http://localhost:8091 --upto M06     # all curl suites
scripts/export-swagger.sh --base http://localhost:8091 --ms M06        # exported Swagger vs contract
scripts/backend.sh stop --port 8091
(cd backend && ./mvnw verify)                                          # JUnit domain tests
```
Expected: all suites pass, `swagger-diff.md` says PASS, tests green.

## 2. Frontend alone (against the contract mock)
```bash
scripts/mock.sh start && scripts/frontend.sh start --api http://localhost:4010
# run loops/frontend-dev/outputs/e2e/Mxx.md with the Playwright MCP against http://localhost:4300
scripts/frontend.sh stop && scripts/mock.sh stop
```

## 3. Full stack (what the verifier does)
```bash
scripts/backend.sh build-jar && scripts/backend.sh start --port 8090 --db stable --fresh --jar run-artifacts/backend-latest.jar
scripts/frontend.sh start --port 4300 --api http://localhost:8090
```
Then walk the end-to-end journey (acceptance.md, scenario E2E-M07-01): create and complete a task; create and complete a habit; add a learning card with milestones; build a plan from them starting about a minute from now; see the start banner and the rest time count down; tick the items; see the plan reach 100% / Completed and the dashboard numbers follow. Expected: no console errors, no 5xx responses.

## 4. Whole pipeline
```bash
scripts/pipeline.sh        # M01..M07, BE || FE -> verify -> commit + tag
```
