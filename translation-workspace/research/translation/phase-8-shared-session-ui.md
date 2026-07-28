# Phase 8 shared Translation session UI

Date: 2026-07-28

## Ownership

`:translation:ui` owns two cohesive responsibilities:

- a provider-neutral `TranslationSessionController` that turns transient text selections into latest-session-wins
  preparation and execution state;
- reusable Compose presentation that chooses an anchored popup or adaptive sheet from that state.

Readers and the Settings test consumer submit `TranslationSessionInput`. They do not call engine SPI, inspect engine
IDs, or duplicate provider state handling.

The controller depends only on `TranslationFeature`. Provider setup, language pickers, profile-default writes, and
documentation navigation remain host actions emitted by the UI because those operations are not part of the
app-facing execution API. Phase 9 will bind them to the existing runtime registries and preferences. This keeps
internal SPI out of reader consumers.

## Session lifecycle

- A changed selection cancels the previous controller job and waits 250 ms before preparation.
- A monotonically increasing generation prevents a cancelled but non-cooperative provider call from publishing stale
  state.
- An anchor-only update changes placement without re-preparing or re-translating identical text.
- Ready providers with `Immediate` invocation execute automatically.
- Ready providers with `ExplicitAction` wait in a typed ready state for their declared action.
- A preparation change after execution becomes the latest preparation state; it is not silently retried.
- Retry re-prepares the current request immediately.
- Dismissal, selection removal, host disposal, and controller closure cancel work and replace state with `Hidden`,
  releasing source and result text from controller state.

## Surface policy

The anchored popup is eligible only for preparing, ready, translating, and successful-result states with a usable
screen-space anchor.

The adaptive sheet is mandatory for:

- missing or invalid anchors;
- disclosure, model, or system setup;
- source, target, or engine choice;
- unavailable, rejected, or failed states;
- an explicit Expand action;
- popup content that does not fit above or below the anchor inside safe viewport bounds.

Popup placement is a pure pixel policy tested independently from Compose. It centers on the selection when possible,
clamps horizontally, prefers below the selection, falls back above, and returns no placement rather than clipping.
The Compose host measures real content and promotes to the sheet when the policy returns no placement; it does not use
a character-count heuristic.

## Presentation and privacy

Provider names, invocation labels, attribution, disclosures, and documentation URLs come from API metadata. The UI
does not branch on engine or provider IDs.

Copy is an explicit user action using the platform clipboard. Source and translated text otherwise stay in controller
memory and are not saved, logged, placed in navigation arguments, or made saveable.

The UI module contains no Settings destination and no reader adapter in this phase.
