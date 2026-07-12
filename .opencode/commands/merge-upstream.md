---
description: Plan merging an upstream Mihon release
agent: plan
---
Use the `upstream-release-merge` skill to inspect and plan merging upstream Mihon release `$ARGUMENTS` into this fork.

Resolve the argument as an exact tag from the `upstream` remote. This planning invocation may fetch that exact tag into a namespaced remote ref, but it must not create a branch, modify source files, start a merge, or resolve conflicts. Present the proposed merge and conflict-resolution plan, then stop for explicit user approval in a later turn and a mode that permits repository mutations.

Treat Katari identity and its dual release distribution as fork invariants: telemetry-enabled universal and ABI-specific APKs plus a universal FOSS APK.
