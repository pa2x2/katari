---
name: merge-upstream
description: Inspect, plan, execute, and validate an ancestry-preserving merge of the upstream Mihon main branch into the Katari fork's current branch. Use when asked to merge, sync, or port changes from the `upstream` Git remote, including executing an approved upstream merge plan.
---

# Merge upstream

Use two phases: inspect and plan first, then execute only after explicit approval. Preserve upstream Git ancestry with a real merge; do not implement upstream changes as unrelated cherry-picks. The merge source is always the `main` branch of the `upstream` remote; the merge base is whichever branch is currently checked out.

## Inspect and plan

Remain in Plan mode when the active surface supports it. Keep the worktree and branches unchanged. Fetching `main` from `upstream` into its remote-tracking ref is the permitted metadata-only exception.

1. Verify the repository root, current branch, worktree state, and the URLs for `origin` and `upstream`. Do not stash, discard, overwrite, or incorporate unrelated changes. Record the current branch name and its HEAD commit as the fork base.
2. Resolve the merge source from the remote-tracking ref, never from an unqualified local branch. Run `git fetch upstream main`, record the exact commit of `refs/remotes/upstream/main` as the target, and compute its merge base with the fork base. Stop if the remote or the branch cannot be verified.
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
   - repository state, fork base branch and commit, exact target SHA, merge base, and comparison ranges;
   - upstream change scope and affected subsystems;
   - a table of classifications, overlapping files, semantic risks, and proposed resolutions;
   - an ordered merge and validation procedure;
   - unresolved decisions, expected conflicts, and explicit exclusions.

Stop after the plan. Require explicit user approval in a later turn before creating a branch, changing files, starting the merge, or resolving conflicts.

## Execute an approved plan

Proceed only when the thread contains the plan, the user explicitly approved it, material decisions are resolved, and the active mode permits mutations.

1. Fetch `upstream main` again. Stop and re-inspect if the target SHA, the current branch identity, or its HEAD commit differs from the approved plan.
2. Protect the active checkout. Never stash or discard user work automatically. Require the primary worktree to be clean, and stop if `upstream-sync/<date>` already exists. In the primary worktree, create and switch to `upstream-sync/<date>` from the current branch HEAD, where `<date>` is today in `YYYY-MM-DD` form:

   ```bash
   git switch -c upstream-sync/$(date +%F)
   ```

   Do not create a linked or sibling worktree. Read `AGENTS.md` again after switching branches.
3. Start an ancestry-preserving merge without committing:

   ```bash
   git merge --no-ff --no-commit refs/remotes/upstream/main
   ```

4. Resolve textual and semantic conflicts according to the approved plan. Do not use blanket `ours` or `theirs` strategies. Preserve fork release identity and architecture unless the approved plan requires a deliberate change.
5. Inspect the complete staged and unstaged result, including files Git merged without conflicts. Confirm that skipped or replaced upstream behavior is intentional and documented in the final report.
6. Run the repository validation sequence defined in `AGENTS.md`. Confirm the release workflow still publishes the telemetry-enabled universal and four ABI-specific APKs together with the universal FOSS APK, using Katari artifact names.
7. Fix in-scope failures and rerun affected checks. If blocked, preserve the active merge state and report it; do not abort and discard resolutions automatically.
8. Commit only after validation succeeds, using `Merge upstream Mihon main` as the merge subject. Do not push, open a pull request, tag a fork release, switch back to the previous branch, or delete the sync branch unless the user separately requests it.
9. Report the sync branch, fork base branch, primary worktree path, merge commit, classifications that required adaptation or exclusion, validation results, and anything not tested.
