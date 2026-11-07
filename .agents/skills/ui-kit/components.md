# Kit components by job

Every public composable in `mihon.presentation.kit`, found by the job it does. Search this file for what the UI does, not for a name. Each component's doc comment in `presentation-core` has the details and its parameters.

The Component column holds only kit composables, in backticks; `checkUiKitBoundaries` checks it against the kit in both directions, so a new kit composable gets its row in the same change.

## Screens and bars

| Job | Component | Notes |
|---|---|---|
| A screen: bars, floating action, snackbars, insets, room for the dock, how wide content grows | `Scaffold` | Pick the content width with its `frame` (see `adaptive-layout`) |
| A screen's top bar, with an action mode while items are selected | `AppBar`, `AppBarTitle`, `UpButton` | `UpButton` takes another icon where "up" closes something, such as a full-screen editor |
| The bar's actions: icons with tooltips, the rest in one "More" menu | `AppBarActions`, `AppBarAvatarButton` | The avatar button ends the actions, such as the active profile |
| Filter chips or a summary line under the app bar, in the same top bar | `AppBarExtension` | Filled like the bar (`actionMode` for a bar in action mode); never a `Surface` or row of your own under the bar |
| A large title that collapses on scroll and grows back | `rememberAppBarScrollBehavior`, `rememberAppBarExpander` | The expander grows it back when content returns to its top from code |
| An app bar that turns into a search field | `SearchToolbar` | |
| An overlay app bar continued by an action on a row beneath it | `OverlayBarRow` | Such as Search all chapters under a reader's search |
| The screen's main action, floating | `FloatingActionButton`, `SmallFloatingActionButton` | The small one is a secondary jump stacked above the main one |
| A prompt and its actions raised from the bottom edge, such as unsaved changes | `BottomActionBar` | |
| Actions on a selection | `SelectionActionBar`, `SelectionAction` | Raised from the bottom in narrow windows, floating from Medium |
| A screen with no app bar names itself to the window | `ReportScreenTitle` | App bars report their own title |
| A screen whose whole content is empty | `EmptyScreen` | A kaomoji, a message and optional actions |
| A screen still loading | `LoadingScreen` | |
| A full screen saying what was opened can't be shown | `ProblemScreen` | `ProblemCard` for a problem inside content that keeps showing |
| A full screen that explains something and asks to accept it (onboarding, crash report) | `InfoScreen` | |

## Navigation

| Job | Component | Notes |
|---|---|---|
| The home destinations | `FloatingDockHost`, `FloatingDock`, `FloatingDockItem` | One dock for the app; screens don't add their own |
| Tabs over a fixed set of pages, such as Browse's sections or a sheet's tabs | `TabRow`, `Tab` | |
| Tabs over pages the user defines, such as library categories | `PillTabRow`, `Tab` | |
| Swipeable pages | `HorizontalPager`, `VerticalPager` | One page per wheel notch or touchpad swipe; never Compose's pagers |
| Which of a few swipeable pages is shown | `PageDots` | |
| A strip of letters along a list's end edge to jump through it | `IndexRail` | |
| A pill of controls floating over content, such as a feed's switcher | `FloatingToolbar`, `FloatingToolbarBox` | |
| A chip floating over a list that jumps through it, such as to new items or back to the top | `FloatingChip` | |

## Rows and row groups

| Job | Component | Notes |
|---|---|---|
| Rows grouped on a card, in a lazy list | `RowGroupItem`, `RowGroupHeader`, `RowGroupGap` | Rows keep their padding against the card's edge; `RowList` scrolls them (see `adaptive-layout`) |
| A short group of rows composed in one piece, outside a lazy list | `RowGroup` | |
| Rows straight on a panel without cards, such as a side panel's editor | `PlainRows` | |
| A label naming rows inside one card that can't split into cards | `RowGroupSubheader` | |
| A line between parts of one row group slice | `RowGroupSeparator` | |
| A note between row group cards: how the rows below are used, or a card's stand-in when it is empty | `RowGroupNote` | |
| A row group card that opens and closes, such as a volume of chapters | `CollapsibleRowGroupHeader`, `CollapsibleRowGroupSubheader` | |
| The base of every row: icon, title, subtitle, trailing control | `SettingsRow` | Use a more specific row below when one fits |
| An on/off setting | `ToggleRow` | Switch on screens, checkbox in sheets, picked by row density |
| One of a set where any number can be picked | `CheckRow` | |
| One of a set where only one can be chosen | `ChoiceRow` | |
| A filter that includes, excludes or ignores | `TriStateRow` | |
| A sort option | `SortRow` | |
| A setting on a slider, its value in a pill | `SliderRow` | |
| A value stepped one meaningful step at a time | `StepperRow` | |
| A labelled field opening a menu of options | `SelectRow` | |
| A row whose body the caller lays out, such as a segmented choice or a pair of buttons | `ControlRow` | |
| Chips under a group header, such as quick filters in a sheet | `ChipsRow` | |
| An explanation or note in a sheet or dialog | `InfoRow` | |
| Options as icon-and-label tiles, such as reading modes | `IconChoiceGrid` | |
| A choice of colour swatches | `ColorChoiceRow`, `ColorSwatches` | Swatches alone where tabs already name the colour |
| A choice of fonts, each title drawn in its font | `FontChoiceRow` | |
| A choice of app themes, each drawn as a small screen | `ThemeChoiceRow` | |
| Sets a sheet's content to compact rows | `ProvideRowDensity` | Sheets set it; rows never pick density themselves |
| A chapter, episode or table-of-contents entry | `ChapterRow`, `ChapterMarks` | |
| A row led by an entry's cover, such as an update or history item | `CoverRow` | |
| A place a search found, with the match highlighted | `SearchResultRow` | |
| A row with actions on a swipe from either edge | `SwipeActionsRow` | |
| A line across a list marking a point in it, such as missing chapters | `MarkerLine` | |
| Tree lines from an expanded item to its children | `Modifier.treeStem`, `Modifier.treeBranch` | |
| Fills a row or item while it is selected | `Modifier.selectionFill` | |
| Pulses an item to point the user at it, such as a setting opened from search | `Modifier.pulsingHighlight` | |

## Containers and surfaces

| Job | Component | Notes |
|---|---|---|
| A rounded container for one item or group | `Card` | |
| A card holding a group of items, such as a shelf of entries; raised to float over content | `GroupCard`, `Modifier.groupCardBehind` | `groupCardBehind` draws the card behind items laid out one by one |
| One choice of a set shown as a card, such as an engine in a picker | `SelectableCard` | The kit owns the selected look |
| A notice or warning among a screen's or sheet's cards, such as an extension no store updates or a paused source | `NoticeCard` | `NoticeTone.Info` or `Warning`; an optional action at its end. Never a `Card` or `Surface` with an icon of your own |
| A banner in a story's colours that opens the story | `StoryBanner` | |
| A dashboard section, its footer and headline tiles | `DashboardCard`, `DashboardCardFooter`, `DashboardTile` | Tiles go in `DashboardTiles` |
| Headline tiles side by side: one line where they fit, two to a line otherwise | `DashboardTiles` | Never a hand-built `Row` of weighted tiles |
| Figures laid out in columns inside a card | `FigureGrid` | |
| Facts on tiles of their own, such as an extension's version and language | `InfoTileRow`, `InfoTile` | |
| A small rounded label for a value | `Pill` | |
| A status as a toned label, such as an engine's readiness | `StatusPill` | |
| A plain fill with no shape, such as a full-screen background | `Surface`, `Modifier.background` | Not for cards, banners or pills: those are named components |
| Content in a container's colours without its fill | `ContentOn` | |
| A container's fill for platform views and the system bars | `SurfaceContainer.fillArgb`, `SurfaceContainer.isLight` | |
| The line between parts of one surface, such as two sides of a translation | `PaneDivider` | |
| A patch of background behind a small control that overlaps content | `Modifier.backgroundHalo` | |
| Fades what lies under an element into the background, such as text cut off mid-line | `Modifier.fadeIntoBackground` | |

## Buttons and actions

| Job | Component | Notes |
|---|---|---|
| The app's button, including destructive confirms | `Button` | Style by its `style` enum and `destructive` |
| An icon button, with a tooltip and shortcut keys on hover | `IconButton`, `IconToggleButton` | |
| Lines between groups of icon buttons in one row, such as a formatting bar | `ButtonGroupDivider` | |
| An item's actions in a row that scrolls or wraps, such as an entry's library and tracking actions | `ActionButtonRow`, `ActionButton` | |
| Equally weighted actions with the icon above the label, such as share and save | `StackedActionButton` | |
| A borderless button around content other buttons can't hold, such as an icon with a label over video | `ContentButton` | Not for an icon and a label in a row: check `ActionButton` and `StackedActionButton` first |
| A chapter's download state as a button | `DownloadIndicator` | |
| Resume an entry from its cover or row, with its progress around the edge | `ResumeButton` | |
| A screen's refresh | `PullRefresh`, `RefreshButton`, `refreshAppBarAction` | Pull to refresh always comes with a button or app bar action |

## Choices and input

| Job | Component | Notes |
|---|---|---|
| Mutually exclusive options as one connected row of equal buttons | `SegmentedButtons` | |
| Options that can each be on or off, drawn as segments | `MultiSegmentedButtons` | |
| A scrolling row of connected toggle buttons, such as content types | `ConnectedToggleRow` | |
| A few connected options inline with other controls | `ConnectedToggleGroup` | |
| One choice of a setting shown as a picture of it | `ChoiceTile` | |
| Bare toggles, outside rows | `Switch`, `Checkbox`, `TriStateCheckbox`, `RadioButton` | Settings use the toggle and choice rows instead |
| A slider | `Slider`, `VerticalSlider` | `SliderRow` for a setting |
| A value stepped one meaningful step at a time | `Stepper` | `StepperRow` for a setting |
| Chips side by side, scrolling or wrapping | `ChipRow` | |
| A chip that starts an action, such as opening a picker | `AssistChip` | |
| A chip that switches a filter on and off | `FilterChip` | |
| A chip for an entered or picked value, with remove | `InputChip` | |
| A chip offering a suggested value | `SuggestionChip` | |
| A chip that includes, excludes or ignores | `TriStateChip` | |
| A text field | `TextField`, `SecureTextField` | |
| A text field with a menu of suggestions | `AutocompleteTextField` | |
| A search field filtering a list or picker, or a query run on demand | `SearchField` | `SearchToolbar` when it lives in the app bar |
| A frameless multi-line text area | `PlainTextField` | |
| A frameless editor for formatted text | `RichTextField` | |
| A date | `DatePicker`, `DatePickerDialog`, `rememberDatePickerState`, `PickerCell` | |
| A time | `TimePicker`, `TimePickerDialog`, `rememberTimePickerState` | |
| A file or folder from the system's picker | `rememberDocumentPicker` | |
| Whether an item is in the selection, shown on the item | `SelectionMark` | |
| Mouse selection and right-click menus over a screen's items | `SelectableItems`, `Modifier.selectableItem` | |
| Drag and drop | `DragPreview`, `DropTargetPanel` | |

## Sheets, dialogs and menus

| Job | Component | Notes |
|---|---|---|
| A sheet | `Sheet`, `SheetHeader`, `SheetActions` | Side sheet in wide windows; actions look like a dialog's. A page opened inside a sheet passes `onBack` and an optional `subtitle` to its header |
| A sheet of settings split into tabs | `TabbedSheet` | |
| A sheet that explains one item, such as an engine or a library | `DetailsSheet`, `DetailsSection` | |
| What a source says about one chapter or episode | `ChapterSheet` | |
| A dialog | `Dialog`, `DialogActions` | |
| Confirm one action | `ConfirmDialog` | |
| A dialog's scrolling content, marked where more lies past an edge | `DialogScrollEdges` | |
| Block the screen while work the user waits for runs | `ProgressDialog` | |
| A window over the whole screen for a viewer | `FullScreenDialog` | |
| A picture full screen, zoomable, dragged down to close | `PictureViewer` | |
| A web page something outside the app puts up, such as an extension's check | `WebPageDialog` | |
| A menu and its items | `DropdownMenu`, `DropdownMenuItem`, `CheckMenuItem`, `RadioMenuItem`, `NestedMenuItem`, `DropdownMenuDivider` | |
| A "More" button opening a menu | `OverflowMenu` | |
| A pill showing a menu's current choice that opens the menu | `MenuPill` | |
| A right-click menu | `ContextMenu` | |

## Feedback

| Job | Component | Notes |
|---|---|---|
| A full-width strip saying what the app or a screen is doing, such as incognito mode or a library update | `StatusStrip` | A tone per mode, an optional wait indicator, progress bar and action; at the top it runs under the status bar (`StatusStripTone.isLight` picks the bar's icons) |
| A list, picker or sheet section with nothing to show | `NoResults` | `EmptyScreen` when the whole screen is empty |
| A problem inside content that keeps showing | `ProblemCard` | |
| Messages and snackbars | `rememberScreenMessages`, `ScreenMessagesHost`, `ScreenMessagesOverlay`, `AppMessagesHost`, `SnackbarHost` | The overlay is for full-window screens without a `Scaffold` |
| A tooltip on long press or hover | `Tooltip`, `HoverTooltip`, `PointerTooltip` | `PointerTooltip` follows the pointer over content that names its own |
| Wait without known progress | `LoadingIndicator` | The app's only indeterminate indicator |
| Progress as a ring or bar | `CircularProgressIndicator`, `LinearProgressIndicator`, `CombinedCircularProgressIndicator` | |
| Progress of long-running work such as downloads | `WavyProgressIndicator` | |
| A small mark on a control for work it stands for, such as translation | `ProgressBadge` | |
| A placeholder for text or a detail still loading | `Modifier.skeleton` | |

## Text, icons and badges

| Job | Component | Notes |
|---|---|---|
| Text | `Text`, `ProvideTextRole` | Pick a `TextRole`; never a text style |
| One text whose parts keep their own tones | `TonedText` | |
| Text that opens what it names | `LinkText` | |
| Read-only formatted text the user wrote | `RichText`, `markdownStyle` | |
| An icon, alone or inside a line of text | `Icon`, `InlineIcon` | |
| A small icon on a round disc marking a step between items | `IconBadge` | |
| Badges, such as on library covers | `Badge`, `BadgeGroup` | |
| A count or dot pinned to an icon or navigation item | `NotificationBadge`, `BadgedBox` | |
| Key caps for a shortcut, such as Ctrl and F | `ShortcutKeyCaps` | |

## Entries, artwork and avatars

| Job | Component | Notes |
|---|---|---|
| A cell of a cover grid around a cover and its title | `CoverCell` | Columns from `coverGridCells` |
| Clip anything to a cover's frame | `Modifier.coverFrame` | |
| How much covers grow in the current window | `entryCoverScale` | Multiply phone-size metrics by it |
| What artwork from data shows while it loads | `rememberArtworkPlaceholder` | |
| Artwork laid faintly behind a header | `ArtworkBackdrop` | |
| An engine's or provider's artwork on a tile | `ArtworkTile` | |
| A logo from data, such as a source's icon | `LogoFrame` | |
| The first letter of a name standing in for a missing icon | `LetterTile` | |
| A profile's avatar | `AvatarTile`, `Avatar` | |
| An image that zooms with wheel, pinch and double tap | `ZoomableImage` | |
| A schematic drawing of a layout or setting | `Modifier.schematic` | |

## Media, readers and players

| Job | Component | Notes |
|---|---|---|
| A pill or panel floating over a full-screen view or media | `OverlayPill`, `OverlayPanel` | |
| A pill for an action that runs by itself once it fills | `OverlayCountdownPill` | |
| A card floating where a provider places it, non-modal | `FloatingPanel` | |
| A card docked to the side over video or pages | `SidePanel` | Modal, with a scrim and a title row: `OverlayDrawer` |
| A modal side drawer over a reader or player, such as its episode list or settings | `OverlayDrawer`, `OverlayListDrawer`, `OverlayDrawerHeader` | `OverlayListDrawer` adds the title row for a list |
| A tab tucked into a screen edge bringing hidden controls back | `EdgeTab` | |
| The background where media is loading or failed | `MediaBackdrop` | |
| A shade behind controls along an edge of media | `MediaScrim` | |
| Text straight over pages or video | `OutlinedMediaText` | |
| A row of tool buttons over media whose labels drop out when it runs short of room | `MediaToolbar`, `MediaToolbarButton` | `Modifier.mediaToolbarSlot` puts a button at the end or orders its label's dropping |
| A playback position bar over video | `MediaTimeline` | |
| A player's volume among its controls | `MediaVolumeControl` | |
| A level a gesture is changing, such as brightness | `MediaLevelIndicator` | |
| What touch, mouse or keys do in a viewer, the first time it opens | `InputGuide` | |
| Where a reader's tap zones are | `TapZoneOverlay` | |
| The surface pages are read on | `PageCanvas` | |
| Marks for recognised text on a page | `PageTextMarks` | |
| Running text in a readable column | `ReadingColumn` | |
| Two pages side by side | `PageSpread` | |
| A long strip of page images | `PageStrip` | |
| A preview of reading colours | `ReadingPageSample`, `ReadingPaletteSwatch` | |

## Dashboards, charts and calendars

| Job | Component | Notes |
|---|---|---|
| Clips for chart marks | `Modifier.chartBar`, `Modifier.chartCell`, `Modifier.chartLegendSample` | Colours from `KatariTheme.chart` |
| The dot beside a legend's label | `LegendSwatch` | |
| A day of a calendar month with its events | `CalendarDayCell`, `EventDots` | |

## Lists, grids and panes

See the `adaptive-layout` skill for which to use at which window size.

| Job | Component | Notes |
|---|---|---|
| A vertical list of rows, and what stays above it | `RowList`, `RowListHeader` | Never a raw `LazyColumn`; `stickyRowGroupHeader` in its scope keeps a header at the top while its rows scroll |
| Entries shown as rows by a grid | `RowGridColumn` | |
| Columns for a cover grid, and how many a grid lays out | `coverGridCells`, `GridCells.columnCount` | |
| Cards of varying height in columns | `CardFlow` | |
| A lazy list or grid with a mouse scrollbar | `ScrollbarFrame` | |
| List and detail side by side | `TwoPanes` | |

## Mouse, keyboard and window

| Job | Component | Notes |
|---|---|---|
| The hand cursor over something a click acts on | `Modifier.clickCursor` | |
| A keyboard shortcut while something is shown | `KeyboardShortcutHandler`, `KeyboardShortcutScope` | |
| The window's layout facts | `currentWindowLayout` | |

## App roots and theming

Composed once by the app or a host, not by screens.

| Job | Component | Notes |
|---|---|---|
| The app's theme | `KatariTheme`, `KatariPreviewTheme` | |
| A reading surface or story themed from a palette | `ReadingPaletteTheme`, `StoryTheme` | |
| The window's layout and keyboard shortcuts | `ProvideWindowLayout`, `KeyboardShortcutsHost` | |
