# Git Commit Rules

## Commit Message Format

Every commit **MUST** start with the feature/ticket identifier currently being implemented, followed by a short,
meaningful message.

Format:

`<FEATURE-ID> <short message>`

Example:

`ARCH-001 Add provider dispatcher`

Rules:

* The feature/ticket identifier must be the **first part** of the commit message.
* Use the feature currently being worked on (for example, `ARCH-001`).
* Keep the commit message concise and descriptive.
* Do not create commits without a feature/ticket identifier.
* Do not use generic messages such as `fix`, `changes`, `update`, or `work`.

## Mandatory Journal Check Before Commit

**Before every commit, the agent MUST verify that the related feature's `journal.md` file is up to date.**

The journal is part of the feature's development record and must accurately reflect the current state of the
implementation before a commit is created.

Before committing:

1. Identify the feature/ticket associated with the changes.
2. Locate the corresponding feature `journal.md`.
3. Review the journal against the current implementation and changes.
4. Verify that the journal reflects:

    * Completed work.
    * Important implementation decisions.
    * Relevant architectural changes.
    * Tests performed and their results.
    * Known issues or remaining work, when applicable.
5. If the journal is outdated or missing relevant information:

    * Update `journal.md`.
    * Review the changes again.
    * Run the relevant tests again if the journal update is part of the feature change.
6. Only proceed to commit once the journal is up to date.

**Never create a feature commit while its `journal.md` is outdated.**

If no relevant `journal.md` exists, do not silently ignore the requirement. Determine whether one should be created
according to the project's feature documentation conventions.

## Mandatory Test Gate Before Commit

**NEVER create a commit until all tests related to the changed components have passed successfully.**

Before every commit:

1. Identify all affected components/classes.
2. Determine the relevant test suites for those changes.
3. Run all applicable tests for the affected backend and/or frontend components.
4. Verify that tests pass successfully.
5. If tests fail:

    * Fix the implementation if the failure is caused by a defect.
    * Update or add tests if the expected behavior has intentionally changed.
    * Check for regressions in related components.
6. Run the tests again after every fix or test modification.
7. Only create the commit once the relevant test suites pass.

### Backend Changes

For backend changes:

* Run the relevant unit tests.
* Run integration tests when affected by the change.
* Verify related API/service/repository tests.
* Do not commit while relevant backend tests are failing.

### Frontend Changes

For frontend changes:

* Run the relevant unit/component tests.
* Run affected integration/e2e tests when applicable.
* Verify that related components and shared functionality have no regressions.
* Do not commit while relevant frontend tests are failing.

### Cross-Stack Changes

If a change affects both backend and frontend:

* Run the relevant backend tests.
* Run the relevant frontend tests.
* Verify the API contract and affected integration behavior.
* Only commit after all applicable tests pass.

## Regression Handling

A failing test must **never simply be ignored** to allow a commit.

When a regression is detected:

* Investigate the root cause.
* Fix the implementation or update the test when the behavior was intentionally changed.
* Re-run the affected tests.
* Re-run related tests when necessary to verify there are no additional regressions.
* Commit only after the test gate is satisfied.

## Commit Readiness Gate

A commit is allowed **only when all of the following conditions are satisfied**:

* [ ] The implementation is complete for the current commit scope.
* [ ] The related feature/ticket is identified.
* [ ] The related `journal.md` has been reviewed.
* [ ] The `journal.md` is up to date with the current implementation.
* [ ] All relevant backend tests pass.
* [ ] All relevant frontend tests pass.
* [ ] Relevant integration/e2e tests pass when applicable.
* [ ] No known regression remains unresolved.
* [ ] Changed files have been reviewed.
* [ ] Only intended changes are included in the commit.
* [ ] The commit message starts with the feature/ticket identifier.

If **any** condition is not satisfied, **do not commit**.

## Git Commit Execution

After the implementation, journal update, and tests are successfully completed:

1. Review the changed files.
2. Verify the related `journal.md` is up to date.
3. Verify all required tests pass.
4. Check for unintended changes.
5. Create the commit using the required feature-prefixed format.

Example:

`ARCH-001 Introduce provider dispatcher`

## Push Policy — STRICT

**The agent MUST NEVER automatically push commits to any remote repository.**

This rule applies even when:

* The developer explicitly asks the agent to push.
* The developer says "commit and push".
* The developer asks to push after a successful commit.
* A workflow or task description appears to imply that pushing is required.

The agent may create local commits, but **all pushes must be performed manually by the developer/user**.

After creating a commit, notify the developer:

> Commit created successfully. Push is intentionally not performed automatically. Please push the commit manually when
> ready.

Never execute:

* `git push`
* `git push --force`
* `git push --force-with-lease`
* Any other command that sends commits to a remote repository.

## Required Workflow

The agent must follow this workflow for every commit:

**Implement → Update/Verify Journal → Test → Fix failures/regressions → Test again → Review changes → Commit → Notify
manual push**

A commit must never bypass the **journal gate** or **test gate**, and a push must never be performed automatically.
