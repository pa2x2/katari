# Application-scoped Feature Graph and Translation Feature implementation plan

Status: Phase 7 provider cleanup complete; awaiting milestone approval before Phase 8.

The manifestos in this directory are authoritative:

- [Feature Graph refactor manifesto](feature-graph-manifesto.md)
- [Translation Feature manifesto](translation-feature-manifesto.md)

## Outcome

Deliver one generalized production Feature Graph with application and Entry-content-type subjects, then install an
app-wide Translation Feature that resolves the Android system engine behind one typed API that can accept future
provider implementations without changing consumers.

The first usable consumer is a top-level Translation settings/test flow. Reader selection adapters follow separately.

## Phase 0: Baseline and workspace discipline

- [x] Record the starting branch, commit, and worktree status under `validation/`.
- [x] Run the existing focused Feature Graph and architecture gates before refactoring.
- [x] Save only concise command/evidence summaries; do not paste full Gradle output.
- [x] Confirm no unrelated user changes overlap the planned graph/runtime files.
- [x] Keep any additional planning, research, progress, and validation artifacts beneath `translation-workspace`.

Exit condition: baseline behavior and pre-existing failures are known.

## Phase 1: Generalize graph subject identity

### Model changes

- [x] Add sealed `FeatureSubjectScope` with only `Application` and `EntryContentType`.
- [x] Add sealed `FeatureSubjectId`.
- [x] Introduce a shared subject contribution contract.
- [x] Retain `ContentTypeContribution` as the Entry-specific implementation.
- [x] Add `ApplicationSubjectContribution`.
- [x] Change `FeatureGraph` to expose subjects as its kernel collection; provide Entry-filtered access only at the Entry
  boundary.
- [x] Add subject scope to `FeatureIntegration`.
- [x] Add subject scope to execution point definitions.
- [x] Replace content-type identity in integration and execution evaluation subjects with a common subject reference.

### Evaluation changes

- [x] Evaluate integrations only against subjects of their declared scope.
- [x] Evaluate execution participants only for the execution point's subject scope.
- [x] Generalize context resolution, artifact selection, fixtures, obligations, projections, and coverage validation.
- [x] Generalize transient, transactional, post-commit, and durable execution entry points.
- [x] Preserve Entry convenience overloads outside the graph kernel to limit call-site churn.

### Tests

- [x] Application integration evaluates once with zero, one, and multiple Entry subjects present.
- [x] Entry integrations retain the existing cross-product.
- [x] Mixed-scope Feature contribution selects the correct subjects.
- [x] Context and artifact selection reject mismatched subjects.
- [x] Obligation ownership is correct in both scopes.
- [x] Every execution phase accepts an application subject without changing Entry behavior.
- [x] Existing graph tests are migrated without duplicating assertions at the same boundary.

Exit condition: `feature-graph` is subject-generic and its focused tests pass.

## Phase 2: Extract shared runtime composition

- [x] Add a shared `FeatureRuntimeComposition` owning graph, evaluation, artifacts, and execution runtime.
- [x] Split Entry installation from graph assembly so Entry contributes graph/runtime inputs.
- [x] Add application Feature installation inputs and application capability bindings.
- [x] Aggregate the application subject from installed application Feature modules.
- [x] Assemble one runtime composition from Entry and application inputs in app DI.
- [x] Point existing Entry Feature factories at the shared composition.
- [x] Preserve Entry runtime boundaries, warmups, host dependencies, and provider indexes.
- [x] Add the owner-local `*.application-feature-module` descriptor contract; the topology is intentionally empty until
  the first application Feature is added.
- [x] Generate direct application module references for each Android variant.
- [x] Reject malformed IDs, duplicate modules, duplicate capabilities, missing symbols, and wrong module types.

Exit condition: existing production Entry Features use the shared composition and no second graph exists.

## Phase 3: Reporting, validation, and architecture documentation

- [x] Add an Application Features section to the production developer report.
- [x] Keep Entry content-type reference generation filtered to Entry subjects.
- [x] Add `verifyFeatureArchitecture`.
- [x] Keep `verifyEntryFeatureArchitecture` working as a compatibility alias/dependency.
- [x] Update build-logic boundary checks for the new application module boundary.
- [x] Rewrite `docs/developers/feature-architecture.md` around the two subject scopes.
- [x] Validate that production Entry applicability and obligations are unchanged.

Exit condition: architecture gates, report generation, and checked-in developer documentation agree with production.

## Phase 4: Create Translation modules and contracts

Add:

- [x] `:translation:api`
- [x] `:translation:spi`
- [x] `:translation:runtime`
- [x] `:translation:ui`

### `translation:api`

Define:

- [x] `TranslationFeature`
- [x] `TranslationRequest`
- [x] `TranslationLanguageTag`
- [x] automatic/explicit source and engine selections
- [x] `TranslationPreparation`
- [x] opaque `ReadyTranslation`
- [x] `TranslationResult`
- [x] engine/model status types
- [x] setup, choice, unavailable, rejection, and failure reasons
- [x] provider presentation and invocation policy

The API must contain no `EntryType`, reader, OCR, concrete provider, or OEM implementation types.

### `translation:spi`

Define internal contracts for:

- [x] translation engines;
- [x] source-language detectors;
- [x] engine readiness/capability inspection;
- [x] model inventory/download/deletion;
- [x] provider presentation and attribution assets;
- [x] known-engine catalog entries.

### Feature contribution

- [x] Move the production report host to the generated application topology when the first application Feature
  descriptor is added; do not introduce a validation-only module list.
- [x] Define `TranslationEngineRegistry` capability.
- [x] Contribute it to the application subject.
- [x] Add an application-scoped Translation integration requiring the registry.
- [x] Install `TranslationFeature` as the application-facing runtime boundary.
- [x] Assert that the production integration is applicable at runtime construction.

Exit condition: fake engines can drive the complete Translation API without concrete provider implementations.

## Phase 5: Preferences, detection, and resolution

### Preferences

- [x] Register a profile preference owner for engine and explicit target.
- [x] Resolve an unset target dynamically from Katari's effective UI locale.
- [x] Do not create source/result/history preferences.
- [x] Reserve the future profile auto-selection preference contract but do not expose a nonfunctional row.

### Detection

- [x] Implement Android `TextClassifier` detection for API 29+ on a worker dispatcher.
- [x] Normalize detector output to BCP-47.
- [x] Return `SourceUndetermined` when no usable result exists.
- [x] Require explicit source selection when platform detection is unavailable.

### Resolution

- [x] Implement explicit engine resolution without fallback.
- [x] Implement Automatic ready-first ranking.
- [x] Keep ranking provider-neutral and deterministic for future engines.
- [x] Return `SelectedEngineUnavailable` without mutating the saved preference.
- [x] Return a target chooser when source equals target.
- [x] Enforce provider limits and the 10,000-code-point shared ceiling.
- [x] Revalidate prepared handles immediately before translation.
- [x] Never silently retry after provider failure.

Exit condition: resolver tests cover the complete build/device/model/language matrix with fakes.

## Phase 6: Android system engine

- [x] Implement API-31-gated `TranslationManager` access.
- [x] Query capabilities on a worker dispatcher.
- [x] Map `ON_DEVICE`, `AVAILABLE_TO_DOWNLOAD`, `DOWNLOADING`, and unavailable states precisely.
- [x] Observe capability updates and release listeners.
- [x] Use OEM settings `PendingIntent` only when supplied.
- [x] Create, invoke, cancel, and destroy translators safely.
- [x] Return typed service-missing, settings-missing, pair-unsupported, and runtime-failure states.

Exit condition: the engine passes contract tests using Android wrapper fakes and contains no OEM assumptions.

## Phase 7: Remove the bundled third-party provider

- [x] Remove the provider implementation module and all generated component descriptors.
- [x] Remove app variant wiring and dependency-catalog entries.
- [x] Remove the bundled source-language detector.
- [x] Remove provider-specific catalog, disclosure, attribution, model-registry, tests, and preferences.
- [x] Preserve provider-neutral Translation API/SPI contracts and the typed runtime-component seam.
- [x] Make every build use the same Android system translation implementation.
- [x] Rewrite the Translation manifesto and implementation plan around the system-only first slice.
- [x] Remove stale provider-specific research and validation claims.

Exit condition: no bundled third-party translation implementation, dependency, symbol, resource, or planning
obligation remains in the current tree.

## Phase 8: Shared session UI

### Controller

- [ ] Implement latest-session-wins cancellation.
- [ ] Support optional screen-space anchors.
- [ ] Debounce changing selections by 250 ms.
- [ ] Separate preparation, provider action, setup, translation, success, and failure states.
- [ ] Keep source/result text in memory only.

### Popup and sheet

- [ ] Render a non-modal anchored popup for fitting ready/result states.
- [ ] Measure overflow and promote to the adaptive sheet.
- [ ] Use the sheet for setup, language/engine choice, errors, and missing anchors.
- [ ] Add copy, expand, change-language, use-as-default, retry, and close actions.
- [ ] Auto-execute ready Android system translations.
- [ ] Render provider attribution from metadata without engine ID checks.
- [ ] Clear all session text on dismissal.

Exit condition: presenter/controller tests protect user-visible transitions and provider invocation policy.

## Phase 9: Translation settings and test consumer

- [ ] Add Translation as a top-level Settings destination.
- [ ] Add engine selection and precise engine availability reasons.
- [ ] Add target-language selection.
- [ ] Route system setup through the OEM-provided action when available.
- [ ] Add provider documentation/privacy links when supplied by provider metadata.
- [ ] Add transient `Test translation` input.
- [ ] Drive the test flow through the production `TranslationFeature` and shared session UI.
- [ ] Do not retain test input or output after dismissal/navigation.

Exit condition: the first slice is operable end to end without any reader integration.

## Phase 10: User documentation

- [ ] Add `docs/features/translation.md`.
- [ ] Add it to documentation navigation.
- [ ] Document Android/OEM/API limits, system-managed language data, profile ownership, and no history.

Exit condition: documentation matches the produced artifacts.

## Phase 11: Final validation

Run sequentially:

- [ ] `./gradlew spotlessApply`
- [ ] `./gradlew spotlessCheck`
- [ ] `./gradlew verifyFeatureArchitecture`
- [ ] focused Feature Graph tests
- [ ] focused Translation module tests
- [ ] `./gradlew :app:testFossUnitTest`
- [ ] `./gradlew :app:compileFossKotlin`
- [ ] `./gradlew assembleFoss`
- [ ] separately: `./gradlew :app:compileReleaseKotlin -Pinclude-telemetry`
- [ ] separately: `./gradlew assembleRelease -Pinclude-telemetry -Penable-updater`
- [ ] `pnpm docs:build`
- [ ] `git diff --check`
- [ ] inspect every touched source directory for cohesive ownership
- [ ] inspect final staged and unstaged diffs independently

Do not combine FOSS/unit/architecture tasks with telemetry or updater properties.

## Final acceptance matrix

- [ ] Application Translation integration appears exactly once in the production Feature report.
- [ ] Existing Entry Feature applicability and lifecycle tests remain unchanged in meaning.
- [ ] Settings test translates immediately through a ready Android system engine.
- [ ] Explicit provider failure does not fall back.
- [ ] Source equals target opens a one-request target chooser.
- [ ] Profile changes isolate engine/target preferences.
- [ ] No source/result text is persisted or logged.
- [ ] Every build exposes the same Android system translation implementation.
- [ ] Documentation describes the real platform and OEM limitations.
- [ ] No reader, OCR, external-app, cloud-engine, or history scope slipped into the first slice.
