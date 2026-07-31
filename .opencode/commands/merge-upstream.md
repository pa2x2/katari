---
description: Plan merging upstream Mihon main into the current branch
agent: plan
---
Use the `merge-upstream` skill to inspect and plan merging the `main` branch of the `upstream` remote into the currently checked-out branch of this fork.

This planning invocation may fetch `main` from `upstream` into its remote-tracking ref, but it must not create a branch, modify source files, start a merge, or resolve conflicts. Present the proposed merge and conflict-resolution plan, then stop for explicit user approval in a later turn and a mode that permits repository mutations.

Treat Katari identity and its dual release distribution as fork invariants: telemetry-enabled universal and ABI-specific APKs plus a universal FOSS APK.
