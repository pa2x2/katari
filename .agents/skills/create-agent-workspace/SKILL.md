---
name: create-agent-workspace
description: Create an immediately usable, ignored planning workspace for a requested feature or change. Use when the user asks to create a workspace, planning workspace, feature workspace, or change workspace that records requirements, rules, architecture, decisions, plans, and dated work notes.
---

# Create Agent Workspace

Create the workspace before analysing or implementing the requested change. It is a durable planning artifact, not authority to modify product code, tests, device state, commits, or external systems.

## Create the workspace

1. Resolve the repository root with Git. Outside a Git worktree, use the current directory as the workspace root.
2. Determine the harness label in this order: the active harness identity, an explicit user-provided value, then `unknown_agent`.
3. Derive a lowercase kebab-case feature slug from the user request. Preserve meaningful nouns and omit generic words such as `feature` or `changes` when that makes the name clearer.
4. Create `<root>/.agents-workspaces/<harness>-<feature>/`. Never overwrite an existing workspace. If the desired directory exists, create the next numeric sibling, for example `codex-reader-refactor-2`.
5. Ensure the root `.gitignore` contains exactly `/.agents-workspaces/`. Create the file if needed; do not replace or reorder existing rules.
6. Copy the six templates from [assets/templates](assets/templates) and replace bracketed placeholders with the initial request, resolved paths, harness, and creation date.
7. Report the workspace path and state clearly that the workspace has been created from initial assumptions and will be refined as the work and discussion continue.

## Fill the initial documents

Keep every claim traceable. Place the original request in `README.md`; mark unverified interpretations as assumptions or open questions rather than facts.

- `README.md`: scope, non-goals, status, and navigation.
- `AGENTS.md`: task-specific rules, authorization boundaries, compatibility constraints, and validation reporting expectations. Link authoritative repository instructions instead of duplicating them.
- `ARCHITECTURE.md`: only known current/target ownership, data flow, and compatibility seams. Do not invent an architecture before inspection.
- `DECISIONS.md`: separate accepted user decisions, recommendations, and open choices. Do not silently promote recommendations.
- `PLAN.md`: implementation units, review gates, and definition of completion. Keep implementation units pending until explicitly authorized.
- `NOTES.md`: append dated evidence, commands/checks run, user direction, and discoveries. Do not use it as an unstructured replacement for the plan or decisions.

Create a narrowly named seventh document only when an independently durable concern emerges, such as format compatibility, provider capabilities, migration strategy, or a protocol. Keep it in the workspace root and link it from `README.md`.

## Refine during work

Read the workspace before each related task. Update it when new user direction, architecture evidence, validation evidence, an accepted decision, or a meaningful scope change appears. Preserve dated evidence and the distinction between accepted, recommended, and open items.

The planning workspace is intentionally ignored. Its creation or revision alone does not authorize production edits, tests, device work, commits, pushes, releases, or pull requests.
