# Phase 3 validation, reporting, and documentation

Date: 2026-07-27

## Result

Feature contract validation and developer reporting now use exact Feature subjects instead of assuming every evaluated
subject is an Entry content type. The production report has separate Application Features and Entry sections, and the
new generalized architecture gate is authoritative.

## Subject-generic validation

Contract and execution-contract fixture selection now resolves the exact `FeatureSubjectContribution` referenced by an
evaluation. Sorting uses the stable subject identity. Entry-only `entryContentType` helpers remain available only for
Entry-owned validation contributors and fail clearly if misused with the application subject.

This was necessary before reporting application Features: changing only the renderer would have left contextual
application contracts and application-scoped execution contracts unable to select their fixtures.

## Reporting

`FeatureDeveloperReport` now includes:

- the optional installed application subject and its providers, adapters, and fixtures;
- scope-aware subject references on integrations and execution participants;
- scope-aware obligation subjects;
- the existing Entry content-type inventory.

The renderer emits a distinct `Application Features` section, then Entry-only integration and execution sections.
The current production snapshot contains one application subject, three Entry content types, 44 Features, 22 execution
points, 369 Entry integration evaluations, and no obligations. There are no application Feature integrations yet.

The neutral task is `generateFeatureReport`; `generateEntryFeatureReport` remains a compatibility alias.

## Entry-only documentation

The content-type reference and source SDK consumer-coverage planners explicitly select
`FeatureSubjectScope.EntryContentType`. Optional projection participation also accepts a subject scope, so a future
application-only Feature is not forced to include or exclude an Entry content-type documentation projection.

This keeps existing generated Entry documentation unchanged when application Features enter the graph.

## Architecture gates

`verifyFeatureArchitecture` is the generalized gate. It covers:

- Entry and application Feature boundary checks;
- graph, runtime, validation, documentation, and production Entry tests;
- the generated developer report;
- checked-in Entry documentation projections.

`verifyEntryFeatureArchitecture` remains a compatibility alias to the generalized gate.

The application-module boundary rule requires every production `ApplicationFeatureRuntimeModule` declaration to have
an owner-local descriptor in the same Gradle module. It also rejects descriptors that name no production module and
verifies that the app uses generated application topology.

## First non-empty application topology

The application module topology is still empty in Phase 3. Production validation installs that same empty topology
through the application installer, which is sufficient to verify exactly-one application-subject behavior today.

When Phase 4 adds the first descriptor, the production report host must consume the generated app topology. It must
move to an app-owned validation composition or receive generated modules through an equivalent typed boundary. A
validation-only hand-maintained list is forbidden. This requirement is now explicit in the Phase 4 checklist.
