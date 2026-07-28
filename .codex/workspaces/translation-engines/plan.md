# Translation Engines Implementation Plan

## Objective

Make translation providers first-class, selectable engines. The existing Android
system engine must use the same contribution path as external engines. Missing
provider apps remain visible in settings with an installation action, and no
generic share, clipboard, or full-app fallback is introduced.

## Working branch and checkpoints

- Worktree: `/home/pa2/projects/katari-root/katari-translation-engines`
- Branch: `translation-engines`
- Base: `upcoming`
- Each milestone ends with focused tests, a manifest audit, a clean targeted
  diff review, and a commit.

## Milestone 1: provider contribution foundation

Status: completed and manifesto-audited on 2026-07-28.

1. Introduce a provider contribution contract that owns:
   - the runtime engine;
   - stable catalog/presentation metadata;
   - setup behavior;
   - acquisition and readiness inspection;
   - output mode.
2. Make the registry consume contributions and reject duplicate engine IDs.
3. Migrate Android System Translation into a contribution without changing its
   stable engine ID, preference value, or user-visible behavior.
4. Make host actions expose engine states rather than a static engine catalog.
5. Make settings render the shared state model and generic install/setup
   actions.
6. Add the provider-surface execution outcome to the shared session contract,
   exercised through a fake provider only.
7. Validate focused API/runtime/UI/settings tests and architecture checks.
8. Audit against `manifesto.md`, then commit.

## Milestone 2: LibreTranslate protocol and Offline Translator

Status: completed and manifesto-audited on 2026-07-28.

1. Add a provider-owned module containing a reusable LibreTranslate client.
2. Add the Offline Translator contribution:
   - package `dev.davidv.translator`;
   - F-Droid acquisition link;
   - fixed loopback host `127.0.0.1`;
   - configurable port, default `5000`;
   - `/languages` readiness and capability discovery;
   - `/translate` inline execution;
   - app launch for setup/model management.
3. Keep selection disabled until the app is installed and its local API has
   completed an initial successful readiness check.
4. Preserve a previously selected engine if it later becomes unavailable.
5. Add protocol, package-state, language-mapping, timeout, cancellation, and
   redaction coverage.
6. Validate, audit against the manifesto, and commit.

## Milestone 3: configurable LibreTranslate server

Status: completed and manifesto-audited on 2026-07-28.

1. Add a separately selectable LibreTranslate Server contribution using the
   same client.
2. Add provider-owned configuration for:
   - endpoint;
   - optional API key;
   - test connection.
3. Require HTTPS remotely and permit cleartext HTTP only for loopback
   addresses.
4. Store the API key through provider-scoped Android Keystore-backed encrypted
   storage.
5. Require one successful initial connection before selection; retain an
   existing selection across later temporary failures.
6. Add endpoint validation, credential redaction, persistence, and picker-state
   coverage.
7. Validate, audit against the manifesto, and commit.

## Milestone 4: integration, documentation, and release validation

Status: completed and manifesto-audited on 2026-07-28.

1. Verify BOOK automatic translation uses the selected contribution and becomes
   unavailable honestly when that provider cannot execute.
2. Verify Android System remains the default and first engine.
3. Update translation and BOOK-reading documentation with installation, setup,
   security, privacy, and model-download guidance.
4. Run formatting, architecture, legacy ABI, focused tests, FOSS unit tests, and
   release compilation/assembly in the repository-prescribed separate
   invocations.
5. Perform a final full-manifest audit and commit any final focused integration
   work.

Validation evidence:

- LibreTranslate provider, translation runtime, BOOK interaction, and
  Translation settings focused tests passed.
- `spotlessCheck`, `verifyEntryFeatureArchitecture`, and
  `verifyLegacySourceAbi` passed.
- `testFossUnitTest` passed without telemetry/updater properties.
- Telemetry/updater-enabled `assembleRelease` passed in a separate invocation;
  all five generated release APKs passed APK Signature Scheme v2 verification.
- The VitePress documentation production build passed.
- No emulator or physical device was used.

## Acceptance criteria

- Android System, Offline Translator, and LibreTranslate Server are peers in one
  registry and settings flow.
- Missing Offline Translator is visible but cannot be selected, and exposes a
  working installation link.
- Offline Translator returns inline Katari results through its localhost API.
- LibreTranslate Server returns inline Katari results through a configured
  endpoint.
- No provider is invoked as a fallback and Katari never silently switches the
  selected engine.
- Adding a future inline or popup-surface provider requires a provider
  contribution and build registration, not edits to shared translation UI or
  session control.
- Existing Android System preferences and behavior remain compatible.
