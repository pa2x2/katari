---
name: ui-kit
description: How Katari UI is built from the presentation-core kit — finding the kit component for a job in its components index before writing any, picking text, colour and emphasis by role, containers by job, and adding to the kit when it lacks something. Use whenever work in Katari writes or changes Compose UI (a screen, sheet, dialog, menu, row, card, control or any composable, including a bug fix inside one), adds to `presentation-core`, or when `checkUiKitBoundaries` reports a violation.
---

# UI kit

`presentation-core` is the UI kit: `mihon.presentation.kit` owns colour, typography, spacing, sizes, shape and every component. Screens are built from kit components and `KatariTheme` tokens only.

The kit owns how a screen is put together, not only its pieces. A screen supplies content and behaviour; its structure (bars, how lists are framed and grouped, headers, spacing between sections, sheet and dialog layout, where the main action sits) comes from the highest-level kit component made for that job. Arranging kit primitives into a structure of your own bypasses the kit, even when every primitive and token in it is the kit's.

For window sizes, panes, lists, grids and folding, also follow the `adaptive-layout` skill.

## 1. Find the component before writing UI

1. **Look the job up in [components.md](components.md)**, the index of every public kit composable by the job it does. Search it by what the UI does (warning, figures, banner, choice, wait, empty), not by the name you would give the component: the kit's name is often a different one (`DashboardTiles`, not `TileRow`; `WarningCard`, not `WarningBanner`).
2. **Open an existing screen of the same kind** (another settings screen, sheet, picker, dashboard) and follow how it uses the kit.
3. **Write only content and behaviour outside the kit.** A local composable that maps a domain value onto a kit component's parameters is fine, such as an engine status turned into a `StatusPill`'s text and tone. A local composable that frames, groups, spaces, aligns or measures content is the kit's job: a `Row` of weighted tiles, a `Surface` around padded text, a custom `Layout` or `MeasurePolicy` for a button.
4. **A component you find elsewhere in the app is not precedent.** Check it against the index; if it duplicates a kit component, use the kit one (and replace the duplicate when the change touches it).
5. **Only when no index row fits, add to the kit** (section 3). Say in your reply which index rows you considered and why none fits.

## 2. Picking what the kit offers

- **Text** by its job: `Text(role = TextRole.…)`. A null role takes the enclosing kit component's style.
- **Colour** by role: `tone = ContentTone.…` for content, `SurfaceContainer.…` for fills. Code that draws (charts, markup, schematics) reads its colours from `KatariTheme.chart` or `KatariTheme.markup`.
- **Emphasis** by colour role or `enabled`, never alpha.
- **Spacing and sizes** from `KatariTheme.spacing` and `KatariTheme.sizes`, or a kit component's parameters.
- **Rows** come from the kit `list` package and pick their control from `LocalRowDensity` (a switch on screens, a checkbox in sheets).
- **Sheets, dialogs, menus, app bars and empty states** have one kit component each.
- **A rounded or raised container** is the kit component named for its job (`Card`, `GroupCard`, `WarningCard`, `OverlayPill`, `OverlayPanel`, `ArtworkTile`, …), not a `Surface` with a chosen shape. A plain `Surface` is for a fill without a shape, such as a full-screen background.
- **Lines and labels between content**: the kit's plain dividers and section header are internal. Rows are split by their row group and sections by the gap between cards; any other line or label is the kit component named for it (`RowGroupHeader`, `RowGroupSubheader`, `RowGroupSeparator`, `PaneDivider`, `MarkerLine`, `DropdownMenuDivider`, `DialogScrollEdges`, `ButtonGroupDivider`).
- **Pagers** are the kit's `HorizontalPager` and `VerticalPager`, which turn one page per wheel notch or touchpad swipe. Only the readers in `entry-interactions` turn their own pages.

## 3. Adding to the kit

When a screen needs something the kit lacks, add it to the matching kit package as a semantic component, parameter or token:

- Parameters say what something is, not how it looks: enums such as `ButtonStyle` or `StatusTone`, never colour, shape or text-style parameters.
- Values that come from data, such as user-picked or document colours, go through the narrow content APIs in `kit/content` and `kit/theme`.
- Name the component after its job, with a doc comment that says the job and gives an example of where it is used: agents find components by that description.
- Add a row for it to [components.md](components.md) in the same change. `checkUiKitBoundaries` fails while a public kit composable is missing from the index, or while the index names one that no longer exists.
- Replace the local copies the new component stands for in the same change, so they don't stay around as precedent.

## Enforcement

`checkUiKitBoundaries` runs with `spotlessCheck` (not with `spotlessApply` or `verifyTests`, so run `./gradlew spotlessCheck` before committing UI work). Outside `presentation-core` it rejects:

- Compose Material, and any dependency on or opt-in to it;
- raw `Dialog` and `Popup`;
- numeric dp and sp, `Color(…)`, text styles and font weights, corner shapes, `KatariTheme.colors`, `KatariTheme.shapes`, `Modifier.shadow` and literal alphas;
- Compose's `HorizontalPager` and `VerticalPager` outside the readers;
- the window reads, raw lists and grid cells listed in the `adaptive-layout` skill.

Inside the kit, it checks that [components.md](components.md) names every public kit composable and nothing else.
