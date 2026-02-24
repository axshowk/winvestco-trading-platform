---
description: # AI Agent Operational Workflow Version: 1.0
---

Scope: Repository-Level Autonomous Git Management

---

# 1. Objective

This document defines the mandatory Git workflow for any AI Agent
operating on this repository.

The agent is responsible for:

- Detecting file changes
- Creating safe branches
- Committing using standardized conventions
- Pushing changes
- Opening pull requests
- Respecting CI and branch protection rules
- Avoiding unsafe Git operations

The agent MUST follow this workflow strictly.

---

# 2. Non-Negotiable Rules

The agent MUST NOT:

- Commit directly to main
- Force push
- Rewrite history
- Use git reset --hard
- Use git push --force
- Commit secrets or environment files
- Modify CI/CD workflows without instruction
- Merge without validation passing

If any unsafe condition is detected → STOP immediately.

---

# 3. Execution Flow (Strict Order)

1. Synchronize repository
2. Detect changes
3. Determine branch type
4. Create branch
5. Validate build & tests
6. Stage & commit
7. Push branch
8. Create pull request
9. Wait for CI
10. Merge after validation

---

# 4. Step-by-Step Process

---

## STEP 1 — Sync with Remote

```bash
git checkout main
git pull origin main
```

If pull fails → STOP and report conflict.

---

## STEP 2 — Detect Changes

```bash
git status
```

If:

- No changes → Exit safely.
- Changes detected → Continue.

Agent must analyze:

- Modified files
- Added files
- Deleted files

If sensitive files (.env, secrets, keys) detected → REMOVE from staging.

---

## STEP 3 — Branch Determination

Branch naming convention:

- feature/<short-description>
- fix/<short-description>
- refactor/<short-description>
- chore/<short-description>
- docs/<short-description>
- hotfix/<short-description>

Examples:

- feature/order-engine-validation
- fix/payment-timeout-bug
- refactor/auth-module-cleanup

---

## STEP 4 — Create Branch

```bash
git checkout -b <branch-name>
```

Branch must always be created from updated main.

---

## STEP 5 — Validation Phase (MANDATORY)

Before commit:

- Ensure project compiles
- Run tests
- Run static analysis
- Ensure no secrets are staged

Examples:

Maven:

```bash
mvn clean verify
```

Gradle:

```bash
./gradlew build
```

Node:

```bash
npm run test
```

If validation fails → STOP and report errors.

---

## STEP 6 — Staging & Commit

Stage changes:

```bash
git add .
```

Commit using Conventional Commits standard:

Allowed Types:

- feat: new feature
- fix: bug fix
- refactor: code restructuring
- chore: maintenance
- docs: documentation
- test: test addition
- perf: performance improvement

Commit format:

<type>: short summary (max 72 chars)

Optional body:

- Why change was made
- Impact scope
- Risk level

Example:

```bash
git commit -m "feat: add order validation logic"
```

If change is complex:

```bash
git commit -m "refactor: restructure payment service

Improves separation of concerns.
Reduces circular dependencies.
No behavior change."
```

---

## STEP 7 — Push Branch

```bash
git push origin <branch-name>
```

Never use force.

---

## STEP 8 — Pull Request Creation

PR Title:
<type>: short description

PR Description MUST include:

- Summary of change
- Why change was required
- Modules impacted
- How to test
- Risk level (Low / Medium / High)

Example:

Title:
feat: implement order status validation

Description:
Adds validation layer before order placement.
Prevents invalid state transitions.
Impacts order-service module only.
Tested via unit and integration tests.
Risk: Low.

---

## STEP 9 — CI & Quality Gates

The agent must wait for:

- Build success
- All tests passing
- Code coverage threshold met
- Security scan passed
- Lint checks passed

If ANY check fails:

- Do not merge
- Create fix commit in same branch
- Re-push

---

## STEP 10 — Merge Policy

Allowed:

- Squash and merge (preferred)
- Rebase and merge

Not allowed:

- Direct merge commits
- Bypassing branch protection

After merge:

- Delete remote branch (if safe)
- Sync local main

```bash
git checkout main
git pull origin main
```

---

# 5. Conflict Handling

If merge conflict detected:

1. Pull latest main
2. Rebase branch
3. Resolve conflicts carefully
4. Re-run validation
5. Push updated branch

Never auto-resolve blindly.

---

# 6. Emergency Stop Conditions

Agent must STOP immediately if:

- Protected branch violation occurs
- Tests fail repeatedly
- Build is unstable
- Unexpected large file deletion detected
- Secret exposure detected

---

# 7. Logging Requirements

After every operation, log:

[AI-AGENT-LOG]
Branch:
Commit Hash:
Files Modified:
PR Link:
CI Status:
Timestamp:

Logs must be clear and structured.

---

# 8. Release Workflow (Optional)

If change is production ready:

```bash
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

Versioning must follow semantic versioning:

MAJOR.MINOR.PATCH

---

# 9. Security Safeguards

The agent must automatically prevent committing:

- .env files
- Private keys
- Credential files
- Token files
- Large binary dumps

If detected → abort commit and report.

---

# 10. Operational Philosophy

The agent must act as a disciplined senior engineer:

- Minimal changes
- Clear commit messages
- No risky shortcuts
- Respect repository integrity
- Prioritize stability over speed

If uncertain → STOP and request clarification.

---

# END OF FILE
