# Phase 7 system-only provider cleanup

Date: 2026-07-28

## Decision

The first Translation slice uses only Android system translation. Katari does not bundle a third-party translation or
language-detection SDK.

The rejected provider required branded result UI whose content restrictions cannot be enforced reliably for arbitrary
reader, local-file, extension, or user-selected content. Katari has no authoritative per-selection classification
that could make that restriction safe. Source metadata, genre heuristics, text scanning, and user assertions are not
acceptable substitutes.

This is a product boundary, not a build-flavor workaround. Every build now contains the same Translation provider
composition.

## Removed surface

- the optional provider module and its runtime-component descriptors;
- app variant dependencies and dependency-catalog entries;
- bundled language identification;
- provider-specific engine, setup, model management, disclosure, attribution, and tests;
- provider-specific known-engine catalog behavior;
- device preferences that had no remaining active behavior;
- the now-unused application Feature base-preference-store dependency;
- standard-versus-FOSS Translation differences and their planned documentation obligations.

Ignored Gradle output beneath the removed module is deleted locally as cleanup and is not part of version control.

## Preserved infrastructure

- `TranslationFeature` remains the sole app-facing boundary;
- Translation API and SPI remain provider-neutral;
- opaque prepared handles, revalidation, cancellation, input limits, and no-fallback rules remain;
- generic source-detector and runtime-component contracts remain available for a separately approved implementation;
- generic model contracts remain reserved but have no current settings or runtime manager;
- Android system translation and its OEM setup routing remain unchanged.

## User-visible consequence

On a supported Android device, Katari uses the system translation service. If the service, language pair, or automatic
source detector is unavailable, Katari reports that state and asks for supported setup or explicit language input. It
does not silently invoke another provider.
