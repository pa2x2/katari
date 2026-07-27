# Phase 1 validation — generalized Feature subjects

## Result

Phase 1 is complete. The Feature Graph kernel now models `Application` and `EntryContentType` subjects without a
synthetic content type, nullable identity, or parallel graph.

## Notable implementation evidence

- `FeatureGraph` stores only generic subject contributions.
- Integration, context, artifact, obligation, projection, execution-evaluation, and runtime results carry a
  `FeatureSubjectReference` or `FeatureSubjectId`.
- Integration and execution-point scopes control the exact evaluation cross-product.
- Application execution is covered through inline, transactional, post-commit volatile, and durable phases.
- Entry-only unwrapping and `ContentTypeId` convenience calls live in Entry Interactions SPI, outside the graph kernel.
- Existing Entry consumers and validation/reporting boundaries were migrated to the explicit Entry projection.
- Durable envelope format and participant routing identity were not changed.

## Validation

| Command | Result |
| --- | --- |
| `./gradlew --quiet spotlessApply` | Passed |
| `./gradlew --quiet spotlessCheck` | Passed |
| `./gradlew --quiet :feature-graph:testDebugUnitTest` | Passed, 70 tests |
| `./gradlew --quiet :feature-validation:testDebugUnitTest` | Passed, 10 tests |
| `./gradlew --quiet verifyEntryFeatureArchitecture` | Passed |
| `git diff --check` | Passed |

The architecture command emitted only the JVM class-data-sharing warning.

Developer architecture documentation is intentionally deferred to Phase 3, where reporting and the broader
`verifyFeatureArchitecture` gate will be updated together so the documentation describes the production composition
rather than this intermediate kernel-only state.
