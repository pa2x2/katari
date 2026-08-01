---
name: merge-upstream
description: Inspect, plan, execute, and validate an ancestry-preserving merge of upstream Mihon main or a specified upstream version tag into the Katari fork's current branch. Use when asked to merge, sync, or port changes from the `upstream` Git remote, including executing an approved upstream merge plan.
---

# Merge upstream

Use two phases: inspect and plan first, then execute only after explicit approval. Preserve upstream Git ancestry with a real merge; do not implement upstream changes as unrelated cherry-picks. The merge source defaults to the `main` branch of the `upstream` remote. When the user provides an upstream version tag, use that exact tag instead. The merge base is whichever branch is currently checked out.

## Inspect and plan

Remain in Plan mode when the active surface supports it. Keep the worktree and branches unchanged. Fetching the selected branch or tag from `upstream` is the permitted metadata-only exception.

1. Verify the repository root, current branch, worktree state, and the URLs for `origin` and `upstream`. Do not stash, discard, overwrite, or incorporate unrelated changes. Record the current branch name and its HEAD commit as the fork base.
2. Select and resolve the merge source:
   - With no version tag, run `git fetch upstream main` and resolve `refs/remotes/upstream/main^{commit}`.
   - With a version tag, require exactly one non-empty tag argument. Use it literally; do not add a `v` prefix, normalize it, or select a similarly named tag. Validate it as `refs/tags/<tag>`, verify that exact tag exists on `upstream`, fetch it from `upstream`, and resolve its peeled commit with `refs/tags/<tag>^{commit}`. Stop if the argument is invalid, the tag is absent from `upstream`, the fetched tag disagrees with the advertised upstream tag, or the tag does not resolve to a commit.
   Record whether the selector is `main` or a tag, its exact name, and the resolved target SHA. Compute the target's merge base with the fork base. Never resolve the target from an unqualified local branch or from an unverified local tag.
3. Treat `merge-base(<fork-base>, <target>)..<target>` as the upstream delta and `merge-base(<fork-base>, <target>)..<fork-base>` as the fork divergence. Inspect commits, full diffs, renamed or deleted files, overlapping paths, and likely conflicts. Use `git merge-tree` for additional conflict forecasting when useful, without starting a merge.
4. Trace affected behavior through the current fork rather than assuming paths still have upstream meanings. Inspect `settings.gradle.kts`, relevant build logic, runtime/composition entry points, database migrations, tests, and representative consumers. Use refactor or manifesto drafts only when the user explicitly names them as constraints.
5. Review cleanly mergeable changes as carefully as textual conflicts. Pay particular attention to:
   - fork-specific Entry and profile-aware architecture;
   - source and extension API compatibility;
   - SQLDelight schema and upgrade paths;
   - Gradle, Android, Kotlin, and dependency changes;
   - application identifiers, signing, updater, telemetry, and release workflows;
   - resources, translations, generated inputs, and test infrastructure.
6. Group upstream changes by subsystem and classify each group:
   - `direct`: accept the upstream implementation;
   - `adapt`: preserve upstream intent through the fork architecture;
   - `replace`: retain or extend an existing fork equivalent;
   - `skip`: intentionally inapplicable, with a concrete reason;
   - `defer`: requires a separate user decision.
7. Present the plan with:
   - repository state, fork base branch and commit, target selector and exact name, exact target SHA, merge base, and comparison ranges;
   - upstream change scope and affected subsystems;
   - a table of classifications, overlapping files, semantic risks, and proposed resolutions;
   - an ordered merge and validation procedure;
   - unresolved decisions, expected conflicts, and explicit exclusions.

Stop after the plan. Require explicit user approval in a later turn before creating a branch, changing files, starting the merge, or resolving conflicts.

## Execute an approved plan

Proceed only when the thread contains the plan, the user explicitly approved it, material decisions are resolved, and the active mode permits mutations.

1. Fetch the approved source from `upstream` again, using `main` or the exact approved tag as applicable. Resolve it by the same procedure used during planning. Stop and re-inspect if the selector no longer resolves to the approved target SHA, or if the current branch identity or its HEAD commit differs from the approved plan.
2. Protect the active checkout. Never stash or discard user work automatically. Require the primary worktree to be clean, and stop if `upstream-sync/<date>` already exists. In the primary worktree, create and switch to `upstream-sync/<date>` from the current branch HEAD, where `<date>` is today in `YYYY-MM-DD` form:

   ```bash
   git switch -c upstream-sync/$(date +%F)
   ```

   Do not create a linked or sibling worktree. Read `AGENTS.md` again after switching branches.
3. Start an ancestry-preserving merge without committing:

   ```bash
   git merge --no-ff --no-commit <approved-target-sha>
   ```

4. Resolve textual and semantic conflicts according to the approved plan. Do not use blanket `ours` or `theirs` strategies. Preserve fork release identity and architecture unless the approved plan requires a deliberate change.
5. Inspect the complete staged and unstaged result, including files Git merged without conflicts. Confirm that skipped or replaced upstream behavior is intentional and documented in the final report.
6. Run the repository validation sequence defined in `AGENTS.md`. Confirm the release workflow still publishes the telemetry-enabled universal and four ABI-specific APKs together with the universal FOSS APK, using Katari artifact names.
7. Fix in-scope failures and rerun affected checks. If blocked, preserve the active merge state and report it; do not abort and discard resolutions automatically.
8. Commit only after validation succeeds. Use `Merge upstream Mihon main` as the merge subject for a `main` merge, or `Merge upstream Mihon <tag>` for a tagged merge. Do not push, open a pull request, tag a fork release, switch back to the previous branch, or delete the sync branch unless the user separately requests it.
9. Report the sync branch, fork base branch, primary worktree path, target selector and SHA, merge commit, classifications that required adaptation or exclusion, validation results, and anything not tested.
