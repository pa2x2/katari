# Phase 4 Translation contracts and runtime

Date: 2026-07-28

## Module boundaries

- `:translation:api` owns the app-facing request, preparation, execution, language, presentation, model, and failure
  vocabulary.
- `:translation:spi` owns internal engine, detector, setup, model-management, registry, and known-engine contracts.
- `:translation:runtime` owns provider-neutral orchestration, registry implementation, Feature Graph contribution, and
  application runtime installation.
- `:translation:mlkit` and `:translation:ui` exist as dependency-direction boundaries. Their production
  implementations remain intentionally deferred to the provider and UI phases.

The API and SPI contain no Entry type, reader, OCR, ML Kit, or OEM implementation type.

## Request and execution authority

`TranslationFeature.prepare(request)` returns typed preparation states. A ready state contains a marker-only
`ReadyTranslation`; the runtime accepts only its own private implementation and rejects forged handles. Execution
rechecks that the prepared engine is still the registry instance for the selected ID, while provider adapters can
return a changed preparation state when device/model readiness changed.

Text is rejected when blank or over the provider limit, with a shared ceiling of 10,000 Unicode code points. The
runtime neither truncates nor chunks. Coroutine cancellation is allowed to propagate.

Phase 4 deliberately proves explicit source/target/engine execution and the single-engine automatic seam with fakes.
The complete multi-engine ranking, profile/default target resolution, and detector selection policies remain Phase 5
work and must replace the provisional no-engine/multiple-engine outcomes before real providers are installed.

## Application Feature

The owner-local `mihon.translation.application-feature-module` descriptor installs:

- one `TranslationEngineRegistry` capability on the application subject;
- one application-scoped `translation.engine-registry` integration;
- one `TranslationFeature` runtime boundary;
- one runtime graph validator requiring the integration to be applicable.

Production warmup validates installed application runtime boundaries and graph validators. The app-owned developer
report uses the generated application topology and discovers both Entry and Translation contract validators through
test fixtures. It reports the Translation integration as applicable with zero obligations.

The report-host/test-fixture prerequisite was committed separately as `ecbf752e8`
(`(refactor): host feature reporting in app`) so Phase 4 remains a focused Translation change.
