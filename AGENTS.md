# Repo Notes

## Layout
- `app/` is the runtime app. Shared code lives in `core/*`, `data`, `domain`, `presentation-*`, `source-*`, `i18n`, `telemetry`, and `macrobenchmark`.
- Custom Gradle plugins and tasks live in `gradle/build-logic/`. `settings.gradle.kts` enables type-safe project accessors and rejects project-level repositories, so add repos only there and use catalog/accessor entries instead of hardcoded versions or string project paths.

## Source organization
- A source directory must represent one cohesive responsibility. Group files by the feature, bounded context, or runtime layer that owns them; do not accumulate unrelated feature contracts, implementations, providers, and helpers in a module-root directory.
- Keep only genuine module-wide entry points and composition roots at the source root. When a module contains multiple responsibilities, create clearly named subdirectories for them as part of the same change that introduces or exposes the split.
- Mirror the production directory structure in tests so behavior and its coverage remain discoverable together.
- Avoid catch-all directories such as `common`, `misc`, or `utils`. Name structural groups after concrete ownership, and place narrowly shared helpers with the feature that owns their semantics.
- Before finishing a change, inspect every touched source directory. If the new files make ownership harder to understand from the tree alone, reorganize that area before committing rather than leaving cleanup for a follow-up.

## Test quality
- Every test must protect a distinct, durable behavior, compatibility guarantee, failure mode, or architectural invariant. Do not retain tests whose only purpose was to observe an implementation while it was being developed.
- Before adding a test, inspect existing unit, integration, regression, and contract coverage for the same behavior. Do not repeat the same assertion at the same boundary; overlapping tests must each protect a distinct regression or integration seam.
- Test observable behavior through the narrowest stable boundary that owns it. Avoid assertions about internal intermediate state, private control flow, or paths that supported production callers cannot reach.
- Avoid tests for pass-through wrappers or exact collaborator call sequences unless the wrapper adds meaningful branching, transformation, ordering, transaction, or error semantics.
- A test must be capable of failing for a plausible regression. Avoid tautological assertions, duplicating production calculations in the test, or snapshots of incidental structure with no reviewed compatibility requirement.
- Do not promote incidental representations, historical implementation details, or arbitrary mock outputs into product contracts. Assertions about exact presentation, persistence, sequencing, or compatibility policy must be traceable to an established requirement, supported prior behavior, or an authoritative producer contract.
- Mocks and fakes must preserve the real collaborator's documented guarantees and supported output space. Do not manufacture inputs that production cannot emit merely to exercise consumer-side normalization; test that invariant at the boundary that actually owns it.
- Before finishing a change, re-read every added or modified test and remove or narrow cases that are now subsumed by stronger coverage introduced later in the same change.

## Toolchain
- Android SDK/NDK and Java compatibility come from `gradle/mihon.versions.toml` plus build logic; do not hardcode them per module.
- Do not run commands in a way that will result in the full output from gradlew to prevent context from being filled with unnecessary information. Use --quiet when actual command output is not needed

## Validation
- Run `./gradlew spotlessApply` from the repo root.
- CI runs `spotlessCheck` -> `verifyEntryFeatureArchitecture` -> `verifyLegacySourceAbi` -> `testFossUnitTest` -> `verifySqlDelightMigration` -> `assembleRelease -Pinclude-telemetry -Penable-updater`.
- App unit tests run on the `foss` buildType (`testBuildType = "foss"`); focused example: `./gradlew :app:testFossUnitTest --tests '...'`.
- FOSS compilation can be verified with `:app:compileFossKotlin`; telemetry-enabled release compilation uses `:app:compileReleaseKotlin -Pinclude-telemetry`.
- Never combine FOSS/unit/architecture tasks with `-Pinclude-telemetry` or `-Penable-updater` in the same Gradle invocation. Those project properties affect every configured variant and can make `processFossGoogleServices` reject the `app.katari.foss` application ID. Run FOSS checks without telemetry/updater properties, let that invocation finish, then run telemetry-enabled release compilation or assembly in a separate invocation.
- Do not infer the installable variant for emulator/device validation from the `foss` unit-test buildType. Before installing, identify the package that is actually running and use the matching Gradle variant: `installDebug` installs `app.katari.dev`, while `installFoss` installs the separate `app.katari.foss` application. After installation, verify that the intended package was launched and that its process changed or restarted; installing a different application ID does not update the app under test.
- After touching `data/src/main/sqldelight`, run `./gradlew verifySqlDelightMigration`.
- Briefly verify if applied changes require docs update. If there's no docs coverage - ask user if docs should be updated

## Generated Files
- Edit `app/src/main/shortcuts.xml`; the variant task generates `xml/shortcuts.xml` with `${applicationId}` substituted.
- `i18n/src/commonMain/moko-resources/**/strings.xml` and `plurals.xml` drive the generated `@xml/locales_config`; `base` maps to `en`, and empty locale resources are skipped.
- App namespace is `eu.kanade.tachiyomi`, but `applicationId` is `app.katari`.
- Gradle properties toggle build features: `include-telemetry`, `enable-updater`, `include-dependency-info`.
- Releases are tag-driven: `v*` publishes telemetry-enabled universal and ABI-specific APKs plus a universal FOSS APK; `sdk-*` warms JitPack for `source-api`, which JitPack builds on OpenJDK 17 via `:source-api:publishToMavenLocal`.
