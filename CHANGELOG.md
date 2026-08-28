# Changelog

## [1.6.0] - 2026-08-28

### 🧩 Improved

- Reworked the Statistics dashboard with clearer activity and current-library sections, including media-specific progress and insight views, top titles, and session patterns.
- Activity charts now show clearly labeled daily, weekly, monthly, or yearly totals without a trend line.
- Book reader keeps the reading position stable while adjacent chapters load, and lets manual scrolling override pending navigation.
- Adaptive feed layouts and back-to-top controls now track the current window and scroll state, while Statistics, activity history, and the Upcoming calendar refresh their date formatting when the locale changes.
- Legacy extensions now load correctly on older Android versions.

### 🐛 Fixed

- Statistics and activity history no longer let accidental sessions shorter than 10 seconds inflate timed totals; consumption completions from short sessions remain visible in history and completion counts.
- In-app release notes now render GitHub line endings correctly and omit checksum and download-selection text intended for release downloads.
- WebView renderer termination now closes the affected page safely instead of leaving the app in a crashed state.
- External search and share intents now forward only supported data, while app data is explicitly excluded from Android cloud backups and device transfers.
- Upcoming category filters now follow the active profile immediately.

## [1.5.5] - 2026-08-27

### 🧩 Improved

- Statistics activity charts now scroll continuously between adjacent periods, preserve the stopping position, and keep date labels aligned with the displayed data.

## [1.5.4] - 2026-08-26

### 🐛 Fixed

- Statistics current streaks now remain active through an incomplete current day and continue across activity outside the selected date range.

## [1.5.3] - 2026-08-25

### 🐛 Fixed

- Statistics activity charts now keep the first and last activity bars inset from the chart borders for clearer edge spacing.

## [1.5.2] - 2026-08-24

### ✨ Added

- The all-time Statistics view now summarizes precisely recorded time, supports year-to-month drill-down for long histories, and labels chart bars as non-cumulative totals.

### 🧩 Improved

- Activity history sessions now include entry covers, session durations, completion badges, and detailed chapter, episode, or item breakdowns that can be expanded for longer sessions.
- Statistics progress now shows each library status with its count and proportional bar, making the breakdown easier to compare.

## [1.5.1] - 2026-08-24

### ✨ Added

- Added navigation through older and newer activity periods in the Statistics dashboard for 7-day, 30-day, and 1-year ranges, with swipe gestures, previous and next controls, accessibility actions, and a `Today` shortcut.
- Added clear visual treatment for periods before detailed activity tracking began, including `Not tracked` chart regions and period-specific loading and retry feedback.

### 🧩 Improved

- Statistics summary and insight cards now keep equal heights when displayed together.

### 🐛 Fixed

- Translation popups now stay clear of text-selection handles so their actions remain tappable.

## [1.5.0] - 2026-08-23

### 🌟 Highlights

Explore your reading and viewing habits with detailed activity trends, progress, streaks, completions, and media-specific insights in the rebuilt Statistics dashboard.

### ✨ Added

- Added detailed activity statistics for manga, books, and anime, including time spent, sessions, completions, active days, streaks, top titles, and selectable date ranges.
- Added activity drill-downs for individual date ranges and entries, plus a separate view of activity recorded before detailed Statistics tracking began.
- Added library coverage and progress breakdowns by media type, with genre, category, and source insights.

### 🔄 Changed

- Entry backups now preserve and restore detailed activity sessions and completion records used by Statistics.

## [1.4.1] - 2026-08-21

### 🔄 Changed

- Library selection pin actions now reflect the selected entries' current states and are hidden when a selection mixes pinned and unpinned entries.

### 🧩 Improved

- Pinned library display settings now include visual previews, and shelf layouts adapt to available space while showing the next entry when more are available.
- Library grid fast scrolling now tracks actual row sizes, preventing scrollbar jumps.
- Backup restoration now preserves the newer entry's pinned or unpinned state.

## [1.4.0] - 2026-08-20

### 🌟 Highlights

Pin library entries to keep them together at the top of your library, with a choice of tonal-group or horizontal-shelf display.

### ✨ Added

- Added bulk pin and unpin actions for library entries, keeping pinned entries prioritized across library sorting modes.
- Added configurable pinned-entry displays for list and grid layouts, with tonal-group and horizontal-shelf options.

## [1.3.12] - 2026-08-19

### 🐛 Fixed

- Book reader progress indicators now continue through chapter transitions and reach 100% at the chapter's terminal scroll boundary.

## [1.3.11] - 2026-08-16

### 🐛 Fixed

- Viewer settings tabs now remain stable while editing numeric values.

## [1.3.10] - 2026-08-15

### ✨ Added

- Added a book-reader option to keep the screen awake while reading.
- Added a book-reader option to control the standard text-selection menu.

### 🐛 Fixed

- Book reader themes now apply consistently to reader controls, translation popups, and text-selection colors.
- The text-selection menu now stays stable while translation results resize.

## [1.3.9] - 2026-08-15

### ✨ Added

- Added Paper and Dusk themes for the book reader.

### 🧩 Improved

- The book reader table of contents now shows read status, bookmarks, reading progress, and the current chapter.
- Reader controls now use larger touch targets in manga and book readers.

### 🐛 Fixed

- The book reader now shows feedback when an internal section link is missing or an external link cannot be opened.
- Saved per-entry reader settings now apply before reader content is rendered.

## [1.3.8] - 2026-08-15

### 🔄 Changed

- Immersive catalog and feed views now keep the system bars visible instead of hiding them.

## [1.3.7] - 2026-08-14

### ✨ Added

- Added an option to keep the navigation bar visible in the book reader when reader controls are hidden.

## [1.3.6] - 2026-08-14

### 🐛 Fixed

- Book-reader translation popups now remain visible and re-anchor while scrolling without conflicting with text-selection action menus.

## [1.3.5] - 2026-08-14

### 🐛 Fixed

- Book reader progress indicators now reflect the visible scrollable range accurately, preserve the actual resume position independently, and stay within rounded screen corners.

## [1.3.4] - 2026-08-13

### 🐛 Fixed

- Book reader text reflow no longer causes a delayed jump after changing text size.

## [1.3.3] - 2026-08-13

### ✨ Added

- Added adjustable book-reader text size from 80% to 200% in 10% steps, with direct value entry and reading position preservation when text reflows.
- Added configurable book-reading progress, including an option to hide it or choose percentage, edge fill rail, edge position marker, or bottom hairline styles.

### 🧩 Improved

- Immersive video controls now hide automatically after three seconds of active playback and reappear when you interact with playback controls.

## [1.3.2] - 2026-08-13

### 🧩 Improved

- Book downloads are now discovered and reflected in library and update counts progressively, and queued manga or book downloads resume after an app restart unless you explicitly paused them.

### 🐛 Fixed

- Leaving the entry notes editor now preserves the final text edit even when it occurs before the autosave delay completes.
- Translation pickers now size correctly, show a clear no-results message for language searches, and keep compact translation controls aligned.

### ⚡️ Performance

- Improved responsiveness when browsing large libraries, filtering or grouping them, searching merge targets, and loading related entries.

## [1.3.1] - 2026-08-11

### 🐛 Fixed

- Manga and book readers now keep retryable startup failures on screen with a `Retry` action instead of closing immediately.
- Webtoon auto-scroll now stops when you manually scroll, zoom, or drag the reader.
- Library tabs now preserve the selected page separately for each profile when switching profiles.

### 🧩 Improved

- Unread book chapters with saved partial progress now show a percentage read in chapter lists when available.

### ⚡️ Performance

- Improved responsiveness during profile startup and when browsing large libraries or entry chapter lists.

## [1.3.0] - 2026-08-10

### 🌟 Highlights

Listen to selected book passages and translation results with configurable speech engines and voices, while selecting text across whole book chapters for copying, translation, or speech.

### ✨ Added

- Added configurable text-to-speech for selected book passages and translation results, with selectable engines and voices, language-specific voice overrides, pitch controls, playback previews, and network-voice consent.
- Added chapter-wide text selection in the book reader (previously selection was possibly only inside one block).
- Added an optional book-reader status bar setting that keeps the status bar visible while reader controls are hidden.

### 🐛 Fixed

- Opening book-reader settings now preserves the current reading position.
- Translation picker dialogs now adapt their height to their content instead of occupying the full available height.
- Book reader navigation bars now match the reader appearance without an unwanted contrast overlay.

### ⚡️ Performance

- Book chapter transitions no longer interrupt continuous scrolling while adjacent chapters load.

## [1.2.1] - 2026-08-08

### ✨ Added

- Added a `Go to current chapter` action to entry screens that scrolls to the next chapter to
  continue reading and highlights it.

### 🔄 Changed

- Book reader progress stays visible when reader controls are hidden and moves above the controls
  when they are shown.

### ⚡️ Performance

- Improved responsiveness when loading large libraries or entry screens and moving between book
  chapters.

## [1.2.0] - 2026-08-08

### 🌟 Highlights

See supported manga images take shape while they download, with smoother book chapter transitions.

### ✨ Added

- Added optional progressive image loading for supported manga images, showing downloaded portions in
  manga readers and entry previews before the full image is available, including animated previews
  where supported. Experimental and available under Settings -> Advanced -> Progressive image loading.

### ⚡️ Performance

- Removed unnecessary work during Book chapter transition that caused noticeable lag
  for entries with large amount of chapters.

Based on [Mihon 0.20.4](https://github.com/mihonapp/mihon/releases/tag/v0.20.4)

## [1.1.4] - 2026-08-04

### 🐛 Fixed

- Restored the selected grouped library page after relaunch and kept grouped library pages isolated
  when switching profiles.

Based on [Mihon 0.20.3](https://github.com/mihonapp/mihon/releases/tag/v0.20.3)

## [1.1.3] - 2026-08-04

### ⚡ Improved

- Prefetch the first viewport of the next book chapter for smoother chapter transitions.

### 🐛 Fixed

- Restored tracking data correctly from backups.

## [1.1.2] - 2026-08-03

### 🌟 Highlights

Customize library grouping levels while navigating immersive manga and book readers more reliably.

### ✨ Added

- Configure the library grouping hierarchy by enabling and reordering category, entry type, and
  source levels, or view all entries without grouping.

### 🐛 Fixed

- Restored page navigation in immersive manga browsing while an image is zoomed in.
- Prevented chapter transition controls in the book reader from triggering reader tap actions.

## [1.1.1] - 2026-08-02

### 🌟 Highlights

Jump directly to any page while browsing manga in immersive mode with the new page scrubber.

### ✨ Added

- Added a bottom page scrubber to immersive manga browsing for quick navigation with haptic
  feedback while scrubbing.

## [1.1.0] - 2026-08-02

### 🌟 Highlights

Review and resume source migrations while replacement searches run in the background, with clearer
immersive-media loading and more reliable downloads.

### ✨ Added

- Source migration can search replacement entries in the background, with review filters,
  per-entry replacement selection, conflict handling, pause and resume controls, and progress
  notifications.

### ⚡ Improved

- Immersive manga and anime browsing now provides clearer loading and retry feedback, with preview
  backgrounds and download progress for manga pages while they load.

### 🐛 Fixed

- Grouped merged entries into a single merge target instead of listing each member separately.
- Queued BOOK downloads in reading order.
- Cleared selected chapters after they are queued for download.

[Unreleased]: https://github.com/pa2x2/katari/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/pa2x2/katari/releases/tag/v1.6.0
[1.5.5]: https://github.com/pa2x2/katari/releases/tag/v1.5.5
[1.5.4]: https://github.com/pa2x2/katari/releases/tag/v1.5.4
[1.5.3]: https://github.com/pa2x2/katari/releases/tag/v1.5.3
[1.5.2]: https://github.com/pa2x2/katari/releases/tag/v1.5.2
[1.5.1]: https://github.com/pa2x2/katari/releases/tag/v1.5.1
[1.5.0]: https://github.com/pa2x2/katari/releases/tag/v1.5.0
[1.4.1]: https://github.com/pa2x2/katari/releases/tag/v1.4.1
[1.4.0]: https://github.com/pa2x2/katari/releases/tag/v1.4.0
[1.3.12]: https://github.com/pa2x2/katari/releases/tag/v1.3.12
[1.3.11]: https://github.com/pa2x2/katari/releases/tag/v1.3.11
[1.3.10]: https://github.com/pa2x2/katari/releases/tag/v1.3.10
[1.3.9]: https://github.com/pa2x2/katari/releases/tag/v1.3.9
[1.3.8]: https://github.com/pa2x2/katari/releases/tag/v1.3.8
[1.3.7]: https://github.com/pa2x2/katari/releases/tag/v1.3.7
[1.3.6]: https://github.com/pa2x2/katari/releases/tag/v1.3.6
[1.3.5]: https://github.com/pa2x2/katari/releases/tag/v1.3.5
[1.3.4]: https://github.com/pa2x2/katari/releases/tag/v1.3.4
[1.3.3]: https://github.com/pa2x2/katari/releases/tag/v1.3.3
[1.3.2]: https://github.com/pa2x2/katari/releases/tag/v1.3.2
[1.3.1]: https://github.com/pa2x2/katari/releases/tag/v1.3.1
[1.3.0]: https://github.com/pa2x2/katari/releases/tag/v1.3.0
[1.2.1]: https://github.com/pa2x2/katari/releases/tag/v1.2.1
[1.2.0]: https://github.com/pa2x2/katari/releases/tag/v1.2.0
[1.1.4]: https://github.com/pa2x2/katari/releases/tag/v1.1.4
[1.1.3]: https://github.com/pa2x2/katari/releases/tag/v1.1.3
[1.1.2]: https://github.com/pa2x2/katari/releases/tag/v1.1.2
[1.1.1]: https://github.com/pa2x2/katari/releases/tag/v1.1.1
[1.1.0]: https://github.com/pa2x2/katari/releases/tag/v1.1.0
[1.0.0]: https://github.com/pa2x2/katari/releases/tag/v1.0.0
