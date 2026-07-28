# Translation Engines Architecture Manifesto

This document is the checkpoint contract for the translation-engine work. Every
milestone must be checked against every applicable rule before it is committed.
If implementation convenience conflicts with this document, the implementation
must change or the deviation must be explicitly discussed with the user before
continuing.

## Product truth

1. External providers are first-class translation engines, never fallbacks.
2. Katari never silently switches engines.
3. Android System Translation remains the default and uses the same contribution
   architecture as every other engine.
4. A supported missing app remains visible and has an installation action, but
   cannot be selected until it is usable.
5. A provider qualifies only when it returns a result Katari can render or opens
   a verified popup/overlay-style provider surface with equivalent interaction.
6. Generic sharing, clipboard workflows, `ACTION_SEND`, `ACTION_TRANSLATE`,
   `ACTION_PROCESS_TEXT`, and ordinary full-app handoff are not translation
   engines.
7. ML Kit is excluded.

## Architecture boundaries

1. `translation/api` contains provider-neutral product contracts only.
2. `translation/spi` contains implementation contracts for independently owned
   engines and contributions.
3. Provider-specific packages, URLs, HTTP schemas, endpoint rules, settings, and
   language quirks remain in the owning provider module.
4. Shared UI consumes typed engine states and generic actions; it does not
   inspect package names, provider IDs, or engine implementation types.
5. Entry features consume `TranslationFeature` and `TranslationHostActions`;
   BOOK must not acquire provider-specific knowledge.
6. Runtime composition discovers or receives contributions, validates IDs, and
   exposes a deterministic registry. It does not hard-code behavior branches for
   individual providers.
7. Adding or removing a provider may change build composition and provider
   registration, but must not require changes to shared picker, session, or
   entry-feature code.
8. The stable Android System engine ID and stored selection remain compatible.
9. Provider setup is provider-neutral at the shared boundary and
   provider-specific behind that boundary.
10. Tests protect observable behavior and architectural invariants, not static
    source shape or collaborator call sequences.

## State and selection rules

1. Engine catalog presence, acquisition, initial configuration, and transient
   runtime availability are distinct facts.
2. Never delete or rewrite a user's selected engine merely because it becomes
   unavailable or is uninstalled.
3. First selection requires acquisition and successful initial readiness.
4. A later transient failure reports honest unavailability and a recovery
   action while preserving the preference.
5. Automatic translation is active only when the selected engine can currently
   execute.
6. Picker and setup screens refresh state after returning from an external app
   and after configuration tests.
7. Android System remains first in default ordering without special-casing its
   behavior in shared UI.

## Execution and user experience

1. Inline providers return the existing provider-neutral `TranslationResult`.
2. Provider-surface engines report a typed surface-opened outcome; absence of a
   returned translation must never be presented as inline success.
3. Popup/provider-surface launch is explicit and verified. A fake implementation
   may test the contract, but an unverified real app must not be advertised.
4. Errors identify the recovery action without leaking selected text,
   translated text, endpoints containing credentials, or API keys.
5. Provider preparation and language support are determined from authoritative
   provider capabilities, not invented static assumptions.

## Offline Translator rules

1. Integrate package `dev.davidv.translator`.
2. Use its LibreTranslate-compatible HTTP API; do not copy or bind its
   unversioned GPL AIDL contract.
3. Connect only to `127.0.0.1`, with default port `5000`.
4. Allow only a validated numeric port in `1..65535`.
5. Use `/languages` for readiness and supported-language discovery.
6. Use `/translate` for execution and return the result inline.
7. Match exact language tags first. Base-language fallback is allowed only when
   the provider catalog proves the mapping unambiguous.
8. Model installation and API enablement remain provider-owned setup actions.
9. Disclose that text crosses an app boundary over localhost and recommend the
   provider's localhost bind mode.
10. Do not log translation payloads.

## LibreTranslate Server rules

1. Reuse the provider-owned LibreTranslate client; do not embed a server runtime.
2. Require HTTPS for non-loopback destinations.
3. Permit cleartext HTTP only for loopback destinations.
4. Reject credentials embedded in endpoint URLs.
5. Keep optional API keys in the established encrypted secret store.
6. Redact API keys and payloads from diagnostics.
7. Require a successful initial connection before selection.
8. Treat later network outages as transient without erasing selection.

## Compatibility and validation

At each checkpoint:

1. Inspect all touched source directories for coherent ownership.
2. Review the targeted diff and staged diff; do not include unrelated changes.
3. Run focused tests for changed behavior.
4. Run `git diff --check`.
5. Re-read this manifesto and record the audit result in the commit message body
   or checkpoint notes.
6. Commit only if all applicable rules pass.

Before the final checkpoint:

1. Run `spotlessApply` and verify its diff is scoped.
2. Run `spotlessCheck`.
3. Run `verifyEntryFeatureArchitecture`.
4. Run `verifyLegacySourceAbi`.
5. Run `testFossUnitTest` without telemetry/updater properties.
6. Run telemetry-enabled release validation separately.
7. Confirm documentation describes actual shipped behavior and recovery steps.
8. Do not use an emulator or physical device unless the user separately
   authorizes it in the current request.
