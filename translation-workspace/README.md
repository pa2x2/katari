# Translation workspace

This directory is the durable planning and coordination workspace for the application-scoped Feature Graph refactor
and Translation Feature implementation.

Production code, tests, resources, and user documentation still belong in their authoritative repository modules.
Implementation-only artifacts such as design notes, investigation results, validation logs, migration checklists, and
temporary decision records belong under this directory rather than being scattered through the repository.

## Authoritative documents

- [Implementation plan](implementation-plan.md) defines scope, phases, interfaces, validation, and completion criteria.
- [Feature Graph manifesto](feature-graph-manifesto.md) governs the graph and runtime-composition refactor.
- [Translation Feature manifesto](translation-feature-manifesto.md) governs product behavior, provider policy, privacy,
  and consumer-facing APIs.

If an implementation choice conflicts with a manifesto, stop and resolve the conflict before changing production
code. Do not silently reinterpret a decision because a local implementation shortcut is easier.

## Workspace conventions

- Keep every planning, research, progress, and validation file somewhere under this `translation-workspace`
  directory. Do not create work-tracking files or directories elsewhere in the repository.
- Put graph investigation and migration notes under `research/feature-graph/`.
- Put provider/API research under `research/translation/`.
- Put command results and acceptance evidence under `validation/`.
- Record newly discovered decisions in the relevant manifesto before relying on them broadly.
- Keep transient build output in normal Gradle/build directories; do not copy generated artifacts here.
- Do not place secrets, selected text, translated text, API keys, signed URLs, or private user data here.
- Update checklist status in the implementation plan as phases complete.

The implementation plan may be refined as repository facts are discovered. The manifestos change only when the product
or architecture decision itself is intentionally revisited.
