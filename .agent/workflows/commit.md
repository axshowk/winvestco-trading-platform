---
description: Automatically handles code commits using enterprise-grade branch management and conventional commit standards.
---

// turbo-all
1. **Analyze Changes**: Run `git status` and `git diff` to understand the scope of changes.
2. **Branch Strategy**:
   - Determine if the current branch is `main` or a long-lived branch.
   - If on `main`, suggest or automatically create a new feature/fix branch using the naming convention: `feature/description` or `fix/description`.
   - Run `git checkout -b <branch-name>` if a new branch is needed.
3. **Stage Changes**: Run `git add -A` to stage all relevant modifications.
4. **Conventional Commits**: 
   - Generate a commit message following the [Conventional Commits](https://www.conventionalcommits.org/) specification (e.g., `feat: add user auth`, `fix: resolve memory leak`).
   - Ensure the message describes *what* and *why*.
5. **Commit**: Run `git commit -m "<conventional-message>"`.
6. **Next Steps**:
   - Inform the user about the new branch and the commit.
   - Ask if they want to push the branch or create a pull request.
