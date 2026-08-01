---
description: Plan merging upstream Mihon main or a version tag into the current branch
agent: plan
---
Use the `merge-upstream` skill to inspect and plan an upstream merge into the currently checked-out branch of this fork.

The optional upstream version tag argument is: `$ARGUMENTS`

If the argument is empty, plan a merge from the `main` branch of the `upstream` remote. Otherwise, require exactly one tag argument and plan the merge from that exact tag after verifying that it belongs to `upstream`. Do not infer, normalize, or substitute a different tag.

This planning invocation may fetch the selected branch or tag from `upstream`, but it must not create a branch, modify source files, start a merge, or resolve conflicts. Present the proposed merge and conflict-resolution plan, then stop for explicit user approval in a later turn and a mode that permits repository mutations.

Treat Katari identity and its dual release distribution as fork invariants: telemetry-enabled universal and ABI-specific APKs plus a universal FOSS APK.
