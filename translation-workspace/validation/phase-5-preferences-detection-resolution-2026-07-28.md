# Phase 5 preferences, detection, and resolution validation

Date: 2026-07-28

Historical record: the automatic candidate-ranking behavior validated at this milestone was superseded by explicit
engine resolution in `e84547f91`.

This record describes the retained Phase 5 behavior after the Phase 7 provider cleanup. Phase 7 validation contains
the current command evidence.

## Retained behavior

- `:translation:runtime` owns profile preferences, default-target resolution, Android platform detection, typed
  component aggregation, and provider-neutral engine resolution.
- Resolver coverage protects ready-first selection, provider-declared priorities, explicit no-fallback behavior,
  saved-absent behavior, source-undetermined and source-equals-target outcomes, stale-handle revalidation, input
  limits, cancellation, and no retry after provider failure.
- API 29 and newer can use Android `TextClassifier`; a missing platform detector yields a typed source-undetermined
  outcome requiring explicit source selection.
- All build variants now have an empty optional Translation component registry.

## Architecture

The general application runtime-component generator and its duplicate-ID validation remain because they are
provider-neutral Feature infrastructure. Translation does not currently register an optional component.

No emulator or physical device was used.
