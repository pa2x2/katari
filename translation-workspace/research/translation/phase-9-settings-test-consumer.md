# Phase 9 Translation settings and test consumer

Date: 2026-07-28

## Destination and ownership

Translation is a top-level Settings destination and participates in Settings search. It is visible on every build
because the application-scoped Translation Feature is present on every build; device and language-pair limitations
are runtime states shown inside the destination, not reasons to hide it.

The existing `ProfileTranslationPreferences` remains the only persistence owner:

- engine selection is one explicit engine ID;
- target language is the effective app language or an explicit BCP-47 language.

The Settings UI does not create a device-wide Translation preference. Playground source text, per-request source/target
overrides, prepared authority, results, and action state remain in the screen model/session only.

## UI boundary

The app Settings package is the composition host for `:translation:runtime`, `:translation:spi`, and
`:translation:ui`:

- runtime supplies the production `TranslationFeature` and profile preferences;
- SPI supplies the installed/known engine catalog and setup actions at this application composition root;
- the shared UI supplies `TranslationSessionController`, the embedded panel, and the overlay host.

The shared UI remains independent of Settings and provider implementations. The Settings host translates its typed
external actions into language pickers, preference writes, setup registry calls, and documentation navigation.

## Availability

An engine's build availability is shown in the engine chooser. Runtime availability cannot be truthfully summarized
without source and target languages, so it is checked by the playground and rendered through the existing typed
preparation states. This preserves distinct reasons for unsupported OS, missing OEM service, missing system settings,
setup in progress, and unsupported language pairs instead of inventing a single static status.

System setup is opened only through the engine's registered setup adapter, which in turn uses the OEM-provided
`PendingIntent`. Returning to Katari re-prepares the current transient request; Katari does not manufacture a system
settings intent or claim ownership of system language data.

Android 14 and newer require a sender opt-in when a visible app executes a `PendingIntent` that starts an activity.
The bridge supplies that option only for this direct user action: API 36+ uses `ALLOW_IF_VISIBLE`, while API 34-35
uses the then-current `ALLOWED` mode. It never grants an always/background launch.

## Language selection

The target chooser is provider-neutral. It derives determinate BCP-47 language tags from Android's locale catalog,
collapses region-only variants to their base language, preserves explicit scripts, and sorts localized display names.
Engine support is still authoritative and is checked during preparation.

The default target row means Katari's effective app language. Per-request language choices made from a Translation
session do not alter the profile preference. Only the explicit `Use as default` action writes the selected target.

## Test lifecycle

The inline playground owns transient, non-saveable text and explicit source, target, and engine selections. Changes
create normal `TranslationRequest` values and send them through the production `TranslationFeature` via the shared
session controller. Immediate providers execute after the playground debounce; explicit-action providers retain their
declared user action.

Dismissal clears controller state. Navigating away cancels the screen-model scope and clears both source and result
text. Neither value is placed in preferences, navigation arguments, saved instance state, logs, workspace artifacts,
or analytics.
