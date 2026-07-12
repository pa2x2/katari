---
name: upstream-release-merge
description: Inspect, plan, execute, and validate ancestry-preserving merges of a specified upstream Mihon release tag into the Katari fork. Use when asked to merge, sync, port, or assess a particular version from the `upstream` Git remote, including an approved upstream-release merge plan.
---

# Upstream release merge

Use two phases: inspect and plan first, then execute only after explicit approval. Preserve upstream Git ancestry with a real merge; do not implement the release as unrelated cherry-picks.

## Inspect and plan

Remain in Plan mode when the active surface supports it. Keep the worktree and branches unchanged. Fetching only the requested upstream tag into `refs/remotes/upstream/tags/` is the permitted metadata-only exception.

1. Require one release version. Normalize `0.20.1` to `v0.20.1`, but reject branches, arbitrary commits, ranges, and malformed versions.
2. Read `AGENTS.md`. Verify the repository root, current branch, worktree state, and the URLs for `origin` and `upstream`. Do not stash, discard, overwrite, or incorporate unrelated changes.
3. Resolve the version from the remote tag namespace, never from an unqualified local tag. Run:

   ```bash
   python3 .opencode/skills/upstream-release-merge/scripts/inspect_upstream_release.py <version> --fetch
   ```

   Record the requested tag, its immutable commit SHA, the fork base ref, and their merge base. Stop if the remote or tag cannot be verified.
4. Treat `merge-base(<fork-base>, <target>)..<target>` as the upstream delta and `merge-base(<fork-base>, <target>)..<fork-base>` as the fork divergence. Inspect commits, full diffs, renamed or deleted files, overlapping paths, and likely conflicts. Use `git merge-tree` for additional conflict forecasting when useful, without starting a merge.
5. Trace affected behavior through the current fork rather than assuming paths still have upstream meanings. Inspect `settings.gradle.kts`, relevant build logic, runtime/composition entry points, database migrations, tests, and representative consumers. Use refactor or manifesto drafts only when the user explicitly names them as constraints.
6. Review cleanly mergeable changes as carefully as textual conflicts. Pay particular attention to:
   - fork-specific Entry and profile-aware architecture;
   - source and extension API compatibility;
   - SQLDelight schema and upgrade paths;
   - Gradle, Android, Kotlin, and dependency changes;
   - application identifiers, signing, updater, telemetry, and release workflows;
   - resources, translations, generated inputs, and test infrastructure.
7. Group upstream changes by subsystem and classify each group:
   - `direct`: accept the upstream implementation;
   - `adapt`: preserve upstream intent through the fork architecture;
   - `replace`: retain or extend an existing fork equivalent;
   - `skip`: intentionally inapplicable, with a concrete reason;
   - `defer`: requires a separate user decision.
8. Present the plan with:
   - repository state, exact target SHA, merge base, and comparison ranges;
   - upstream release scope and affected subsystems;
   - a table of classifications, overlapping files, semantic risks, and proposed resolutions;
   - an ordered merge and validation procedure;
   - unresolved decisions, expected conflicts, and explicit exclusions.

Stop after the plan. Require explicit user approval in a later turn before creating a branch, changing files, starting the merge, or resolving conflicts.

## Execute an approved plan

Proceed only when the thread contains the plan, the user explicitly approved it, material decisions are resolved, and the active mode permits mutations.

1. Fetch `origin/main` and the exact upstream tag again. Re-run the inspection helper and stop if the target SHA or material repository state differs from the approved plan.
2. Protect the active checkout. Never stash or discard user work automatically. Require the primary worktree to be clean, and stop if `upstream-sync/<tag>` already exists. In the primary worktree, create and switch to `upstream-sync/<tag>` from `origin/main`:

   ```bash
   git switch -c upstream-sync/<tag> origin/main
   ```

   Do not create a linked or sibling worktree. Read `AGENTS.md` again after switching branches.
3. Start an ancestry-preserving merge without committing:

   ```bash
   git merge --no-ff --no-commit refs/remotes/upstream/tags/<tag>
   ```

4. Resolve textual and semantic conflicts according to the approved plan. Do not use blanket `ours` or `theirs` strategies. Preserve fork release identity and architecture unless the approved plan requires a deliberate change.
5. Inspect the complete staged and unstaged result, including files Git merged without conflicts. Confirm that skipped or replaced upstream behavior is intentional and documented in the final report.
6. Run focused checks while resolving issues, then run the repository validation sequence with concise Gradle output:

   ```bash
   ./gradlew --quiet spotlessApply
   ./gradlew --quiet spotlessCheck
   ./gradlew --quiet testFossUnitTest
   ./gradlew --quiet verifySqlDelightMigration
   ./gradlew --quiet assembleRelease -Pinclude-telemetry -Penable-updater
   ./gradlew --quiet assembleFoss -Penable-updater
   git diff --check
   ```

   Confirm the release workflow still publishes the telemetry-enabled universal and four ABI-specific APKs together with the universal FOSS APK, using Katari artifact names.

7. Fix in-scope failures and rerun affected checks. If blocked, preserve the active merge state and report it; do not abort and discard resolutions automatically.
8. Commit only after validation succeeds, using `Merge upstream Mihon <tag>` as the merge subject. Do not push, open a pull request, tag a fork release, switch back to the previous branch, or delete the sync branch unless the user separately requests it.
9. Report the branch, primary worktree path, merge commit, classifications that required adaptation or exclusion, validation results, and anything not tested.
