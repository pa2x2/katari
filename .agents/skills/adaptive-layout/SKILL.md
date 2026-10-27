---
name: adaptive-layout
description: Rules every Katari screen follows so it works at any window size and through folding: where window facts come from, which kit layout types build a screen, insets, state that survives a fold, and the four windows to check. Use whenever UI work in Katari adds or changes a screen, tab, pane, list, grid, sheet, dialog, menu, app bar or navigation, and when `checkUiKitBoundaries` reports an adaptive-layout violation.
---

# Adaptive layout rules

Katari runs on phones, tablets, foldables and resizable windows. A book-style foldable shows a narrow portrait screen folded and a wide, nearly square one unfolded, both portrait, and switches between them while a screen is open. These rules let one screen work in all of them without per-screen fixes. The UI kit (`presentation-core`, `mihon.presentation.kit`) owns every decision below; a screen supplies content and behaviour. They extend the UI kit rules in `AGENTS.md`.

## 1. Window facts come from the kit only

- Read the window through `currentWindowLayout()` (`kit/window/WindowLayout.kt`): `width` (`WindowWidth.Compact` < 600 dp, `Medium` < 840 dp, `Expanded`), `isShort` (< 480 dp tall), `fold` (a `WindowFold`, with its position and whether it's half-open), `usesSideNavigation` and `usesTwoPanes`. The Tablet UI setting ("Never") is already applied to them.
- Never read `LocalConfiguration`'s size or orientation, `LocalWindowInfo`, `Configuration.ORIENTATION_*`, display metrics or `WindowInfoTracker` outside the kit. Orientation says nothing about the room a screen has: folded and unfolded are both portrait.
- Never choose a layout from a width you measured (`BoxWithConstraints` plus a breakpoint). If a kit type should adapt, change the kit type.

## 2. Screens are built from the kit's layout types

| Job | Kit type |
|---|---|
| A screen: bars, FAB, snackbars, insets, room for the floating dock | `kit/navigation/Scaffold` |
| A vertical list of rows (row group cards, history, sources, settings) | `kit/layout/RowList`: rows capped at 640 dp and centred, with a scroll indicator while scrolling; `fastScroll = true` makes it a draggable thumb for long lists (`fastScrollTopPadding` when the first row runs under a transparent bar); `columnInset` in its scope lets a backdrop behind the first rows bleed to the list's edges |
| A grid of entry covers | `GridCells` from `kit/layout/coverGridCells(coverSize, wideCovers)`; the column count follows the user's Cover size, never a fixed count or orientation |
| Entries shown as rows through a grid (list display modes) | `kit/layout/RowGridColumn(contentPadding) { columns, padding -> … }`: one column, centred like `RowList` |
| Cards of varying height (global search sources, statistics) | `kit/layout/CardFlow`: one column, two once two cards fit; a wide card spans with `StaggeredGridItemSpan.FullLine`. Row group and dashboard cards bring their own side margin and gap, so the flow adds none |
| Actions or chips that may not fit on one line | `ActionButtonRow(wrap)`, `ChipRow(wrap)`: scroll sideways in compact windows and wrap where the pane has room, such as an entry's info pane in two panes |
| List and detail side by side | `kit/layout/TwoPanes`, when `usesTwoPanes`: splits on a vertical fold, start pane clear of the side dock |
| Sheet, dialog, menu, selection actions | `Sheet`, `Dialog`, `DropdownMenu`, `SelectionActionBar`: each picks its own form (side sheet in wide windows, one half of a half-open fold, floating selection pill from Medium) |

A raw `LazyColumn`, a `GridCells` or `StaggeredGridCells` built outside the kit, or a hand-built pane split is a structure of your own; use the type above or add one to the kit.

## 3. Navigation

- The home destinations float in one dock: at the bottom in Compact windows (inside `HomeScreen`), at the start edge otherwise, beside every screen of the root navigator (`ui/home/navigation/HomeSideNavigationHost`). Screens get the room through the kit `Scaffold`; don't pad for it.
- A screen that must take the whole window (onboarding, WebView) implements `FullWindowScreen`. Readers and the player are separate activities.
- A home tab that takes the window for itself, such as during a selection, sets `LocalHomeNavigation.current.hiddenByTab`.

## 4. Insets

- The kit pads for system bars, the camera cutout and the dock. Backdrops and artwork run edge to edge (`ArtworkBackdrop(bleed = …)`), and only content is inset.
- Rows under an app bar in the same top bar (filter chips, a summary line) go in `AppBarExtension`, which insets them like the bar's own contents: system bars, cutout and side dock.
- Don't add hand-picked padding for bars or cutouts, and don't consume insets a sibling pane still needs.

## 5. The window changes while the screen is open

`MainActivity` handles size changes in place (fold, unfold, resize, multi-window), so the composition survives, but anything composed from scratch must not reset state.
- Keep open state (dialogs, sheets, selections, search text, scroll) in the view model or `rememberSaveable`.
- Close things only on real events observed where they happen (a profile switch in the view model), never in a `LaunchedEffect` keyed on a value that is simply read again on recomposition.
- Don't cache window facts at screen creation; read them where they are used.

## 6. Check on four windows

Before calling UI work done, look at it in each of these, on a foldable device or a foldable or resizable emulator:

| Window | What it catches |
|---|---|
| Compact portrait (a phone, or a foldable folded) | the bottom dock, bottom sheets, single panes |
| Short and wide (a phone or folded foldable in landscape) | `isShort` layouts, the side dock beside a low window, a side cutout |
| Expanded (a tablet, or a foldable unfolded) | the side dock on pushed screens, two panes on the fold, side sheets, centred row lists |
| Half-open book posture | dialogs and sheets kept to one half, nothing important across the bend |

Also change the window while a sheet, dialog, selection and search are open, by folding or resizing: each must stay open.

## Enforcement

`checkUiKitBoundaries` (runs with `spotlessCheck`) checks Compose files outside `presentation-core` and names this skill in its messages. It rejects:
- window size and orientation reads: `orientation`, `screenWidthDp`, `screenHeightDp` or `smallestScreenWidthDp` from `LocalConfiguration`, `Configuration.ORIENTATION_*`, `LocalWindowInfo`, `WindowInfoTracker` and display metrics (locale, `uiMode` and other configuration reads still pass);
- `LazyColumn`, `FastScrollLazyColumn` and `ScrollbarLazyColumn`;
- `GridCells` and `StaggeredGridCells` `Fixed`, `Adaptive` and `FixedSize`.

Run `./gradlew spotlessCheck` before committing UI work.
