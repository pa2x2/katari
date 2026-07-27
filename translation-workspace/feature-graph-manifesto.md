# Feature Graph refactor manifesto

## Purpose

Generalize Katari's Feature infrastructure so one graph can model both application-scoped Features and
Entry-content-type-scoped Features without weakening the existing Entry architecture.

Translation is the first application-scoped Feature. It is the proving case, not a justification for adding
translation-specific concepts to the graph kernel.

## Core model

The graph has exactly two subject scopes in this refactor:

1. `Application`, evaluated once for the installed application.
2. `EntryContentType`, evaluated once for each installed Entry content type.

The subject identity is sealed and exhaustive. Do not add an open arbitrary-scope registry, synthetic `APP` content
type, nullable content type, magic ID, or type check that pretends application behavior is Entry behavior.

A Feature may own integrations in both scopes. Scope belongs to each integration because a future Translation Feature
may have:

- an application integration for engine infrastructure; and
- Entry integrations for renderer-owned text selection.

Execution points are scoped as well. Applicability, context, artifacts, obligations, contracts, projections, transient
execution, and durable execution must all speak the same subject language.

## Non-negotiable invariants

- There is one assembled production Feature Graph, not an Entry graph plus an application graph.
- An application integration is evaluated exactly once and never participates in a content-type cross-product.
- An Entry integration retains the existing one-evaluation-per-installed-content-type behavior.
- Graph discovery remains contribution-driven. The kernel must not acquire a catalog of concrete Features, Entry
  types, engines, or app modules.
- Provider absence remains valid unsupported behavior. Do not add intentional-absence markers.
- Specialized requirements create obligations only after prerequisites establish applicability.
- Obligation ownership follows the affected subject owner; Feature-owned projection obligations remain Feature-owned.
- Application consumers and Entry consumers call their application-facing Feature. They do not inspect the graph,
  dispatch providers, or reproduce applicability rules.
- Runtime state such as downloaded models, OEM service readiness, connectivity, or user preference is not frozen into
  the static graph.
- Existing lifecycle guarantees must not be weakened while generalizing subject identity.
- Durable execution payload and routing compatibility must be preserved. Do not introduce a persistence migration
  unless a real persisted representation changes.

## Runtime composition

Graph assembly currently lives inside Entry Interactions. Move it to one shared `FeatureRuntimeComposition` that owns:

- the assembled graph;
- static evaluation;
- selected artifacts;
- execution runtime.

Entry runtime installation contributes its existing type plugins, Feature contributors, execution bindings, durable
bindings, runtime boundaries, and warmups.

Application Feature installation contributes:

- application capability bindings;
- application Feature contributors;
- execution bindings if an application Feature eventually needs them;
- runtime boundaries and warmups.

The app composition root combines both domains and creates one runtime composition. Domain installers may remain
domain-specific, but they must feed the same graph and evaluation.

Production registration remains owner-local and generated into direct Kotlin references. Add
`*.application-feature-module` descriptors for application Features. Do not use reflection, `ServiceLoader`,
source-text parsing, or a hand-maintained production module list.

## API migration rules

- Introduce a common sealed `FeatureSubjectId` and subject contribution abstraction.
- Keep `ContentTypeContribution` as the Entry-specific subject contribution rather than deleting useful Entry
  vocabulary.
- Replace graph-kernel `contentType` fields with subject references.
- Put Entry-only unwrapping helpers in the Entry boundary. They must fail clearly if given an application subject.
- Add subject scope to Feature integrations and execution points.
- Existing Entry declarations may use a temporary Entry-scope default during migration. New application declarations
  must be explicit.
- Do not leak `EntryType` into `feature-graph`.
- Do not leak Android, Injekt, Translation, UI, or build-variant types into the pure graph model.

## Reporting and compatibility

- Expand developer reporting with a distinct Application Features section.
- Keep the content-type reference projection restricted to Entry subjects.
- Add `verifyFeatureArchitecture` as the broader architecture gate.
- Preserve `verifyEntryFeatureArchitecture` as a compatible alias/dependency so CI and developer commands continue to
  work.
- Rewrite the developer architecture guide around the two-scope model while preserving the existing Entry lifecycle
  explanations.
- Generated topology and validation must consume the same production registrations used at runtime.

## Test doctrine

Tests must protect behavior or architecture, not the mechanical shape of the refactor.

Required coverage:

- application integrations evaluate once;
- Entry integrations preserve current applicability;
- one Feature can mix application and Entry integrations;
- application subjects cannot enter Entry-only helpers or reports;
- context resolution and artifact selection use exact subjects;
- specialized and fixture obligations name the correct responsible owner;
- application-scoped execution works through every execution phase;
- duplicate subjects, capabilities, modules, and invalid scope/subject pairs fail deterministically;
- the migrated production Entry graph has no changed applicability or unresolved obligations.

Do not add tests that merely count declarations, mirror implementation calculations, scan source text, or assert
incidental data-class formatting.

## Forbidden shortcuts

- A fake content type for the app.
- An `EntryType` branch in the graph kernel.
- A second graph assembled only for Translation.
- App-build checks in Translation consumers.
- A central manual list of application Feature modules.
- Reflection-based runtime discovery.
- Generalizing only Feature integrations while leaving context, artifacts, or execution content-type-only.
- Renaming persisted durable payloads without compatibility evidence.
- Broad unrelated cleanup while touching graph-heavy directories.

## Completion criteria

The refactor is complete only when:

- all production graph consumers compile against subject identity;
- existing Entry behavior and validation remain green;
- application-scoped Translation appears once in the production report;
- the shared runtime composition is the single graph authority;
- old architecture commands still work;
- the developer documentation describes the real runtime;
- no synthetic content type or parallel availability matrix exists.
