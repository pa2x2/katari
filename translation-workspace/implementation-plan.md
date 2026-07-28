# Application-scoped Feature Graph and Translation Feature implementation plan

Status: Phase 6 complete; awaiting milestone approval before Phase 7.

The manifestos in this directory are authoritative:

- [Feature Graph refactor manifesto](feature-graph-manifesto.md)
- [Translation Feature manifesto](translation-feature-manifesto.md)

## Outcome

Deliver one generalized production Feature Graph with application and Entry-content-type subjects, then install an
app-wide Translation Feature that resolves Android system and build-specific ML Kit engines behind one typed API.

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
- [x] `:translation:mlkit`
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

The API must contain no `EntryType`, reader, OCR, ML Kit, or OEM implementation types.

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

Exit condition: fake engines can drive the complete Translation API without Android or Google implementations.

## Phase 5: Preferences, detection, and resolution

### Preferences

- [x] Register a profile preference owner for engine and explicit target.
- [x] Resolve an unset target dynamically from Katari's effective UI locale.
- [x] Store Wi-Fi policy and Google disclosure acknowledgement in the base/device store.
- [x] Do not create source/result/history preferences.
- [x] Reserve the future profile auto-selection preference contract but do not expose a nonfunctional row.

### Detection

- [x] Implement Android `TextClassifier` detection for API 29+ on a worker dispatcher.
- [x] Implement bundled ML Kit detection for standard API 26–28.
- [x] Normalize detector output to BCP-47.
- [x] Return `SourceUndetermined` when no usable result exists.

### Resolution

- [x] Implement explicit engine resolution without fallback.
- [x] Implement Automatic ready-first ranking.
- [x] Prefer Android system when both supported engines are ready.
- [x] Prefer ML Kit setup when neither standard-build engine is ready.
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

## Phase 7: ML Kit engine and strict variant separation

- [ ] Add the version-catalog entry for `translate:17.0.3`; bundled `language-id:17.0.6` was added in Phase 5.
- [x] Put current ML Kit dependencies only on standard/debug/preview/benchmark configurations.
- [x] Use variant source composition so FOSS never references ML Kit symbols.
- [ ] Implement language-pair support, readiness, translation, cancellation, and deterministic close.
- [ ] Implement downloaded-model inventory, pre-download, delete, and progress.
- [ ] Estimate storage from missing language models at approximately 30 MB each.
- [ ] Enforce Wi-Fi by default and support one explicit metered override.
- [ ] Add the one-time provider/download disclosure.
- [ ] Return explicit `Translate with Google` invocation policy.
- [ ] Return official adjacent result attribution and disclaimer metadata.
- [ ] Keep the known ML Kit catalog entry available in FOSS without implementation loading.

Exit condition: standard provider contracts pass and FOSS runtime classpath contains no ML Kit artifact.

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
- [ ] Require the ML Kit labeled action before execution.
- [ ] Render provider attribution from metadata without engine ID checks.
- [ ] Clear all session text on dismissal.

Exit condition: presenter/controller tests protect user-visible transitions and provider invocation policy.

## Phase 9: Translation settings and test consumer

- [ ] Add Translation as a top-level Settings destination.
- [ ] Add engine selection and precise engine availability reasons.
- [ ] Add target-language selection.
- [ ] Add device-wide Wi-Fi-only policy.
- [ ] Add model inventory, pre-download, delete, estimated size, and progress.
- [ ] Add provider/privacy/disclaimer links.
- [ ] Show ML Kit disabled in FOSS as not included in the build.
- [ ] Add transient `Test translation` input.
- [ ] Drive the test flow through the production `TranslationFeature` and shared session UI.
- [ ] Do not retain test input or output after dismissal/navigation.

Exit condition: the first slice is operable end to end without any reader integration.

## Phase 10: User documentation and releases

- [ ] Add `docs/features/translation.md`.
- [ ] Add it to documentation navigation.
- [ ] Update `docs/differences/builds-telemetry-and-privacy.md`.
- [ ] Document standard ML Kit inclusion, FOSS system-only behavior, OEM/API limits, model storage, SDK diagnostics,
  profile/device ownership, attribution, and no history.
- [ ] Update `.github/workflows/release.yml` so every generated release body warns about engine differences.
- [ ] Link releases to
  `https://katariapp.github.io/katari/differences/builds-telemetry-and-privacy#translation`.

Exit condition: documentation and release messaging match the produced artifacts.

## Phase 11: Final validation

Run sequentially:

- [ ] `./gradlew spotlessApply`
- [ ] `./gradlew spotlessCheck`
- [ ] `./gradlew verifyFeatureArchitecture`
- [ ] focused Feature Graph tests
- [ ] focused Translation module tests
- [ ] `./gradlew :app:testFossUnitTest`
- [ ] `./gradlew :app:compileFossKotlin`
- [ ] inspect `fossRuntimeClasspath` for ML Kit absence
- [ ] inspect the FOSS APK for ML Kit classes/resources
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
- [ ] ML Kit cannot execute without `Translate with Google`.
- [ ] ML Kit results carry adjacent official attribution.
- [ ] Missing models show languages, estimate, Wi-Fi policy, and explicit download.
- [ ] One-download mobile-data override does not change global policy.
- [ ] Explicit provider failure does not fall back.
- [ ] Source equals target opens a one-request target chooser.
- [ ] Profile changes isolate engine/target preferences.
- [ ] Models and disclosure remain device-wide.
- [ ] No source/result text is persisted or logged.
- [ ] FOSS contains no ML Kit implementation or dependency.
- [ ] FOSS still explains ML Kit as unavailable in this build.
- [ ] Documentation and release template disclose the build difference.
- [ ] No reader, OCR, external-app, cloud-engine, or history scope slipped into the first slice.
