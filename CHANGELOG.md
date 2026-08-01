# Changelog

## [1.7.0] - 2026-08-01

### 🌟 Highlights

A rebuilt native document reader provides continuous, selectable HTML book reading with
configurable themes, while migrations and source filtering handle larger collections better.

### ✨ Added

- Sources can provide paged, searchable filter groups with optional jump navigation for very
  long filter lists.
- Migrate individual entries or whole groups from merged library entries, and open a specific
  source member directly for inspection.
- Open the default manga or document-reader settings directly from an active reader.

### 🔄 Changed

- Compatible HTML book chapters now use a native document reader with continuous chapter
  reading, table-of-contents navigation, selected-text translation, and a per-book black theme.

### 🗑️ Removed

- EPUB reader support; compatible book sources must provide semantic BOOK documents to remain
  readable in Katari.

### 🐛 Fixed

- Kept library membership and the original added date intact when an entry is refreshed from its
  source.
- Kept next-chapter preparation and automatic selected-text translation settings independent for
  manga and the document reader, including per-entry overrides.

## [1.6.2] - 2026-07-30

### 🌟 Highlights

Selected-text translation follows your selection as you scroll, and the EPUB reader settles
on a single, consistent paginated layout.

### ⚡ Improved

- Made reader and app icons clearer and more accessible, with reading-mode layout icons in
  the prose reader and state announcements for reader and download controls.

### 🗑️ Removed

- The EPUB reader no longer offers a continuous-scrolling layout; reflowable EPUBs are now
  always paginated.

### 🐛 Fixed

- Kept the selected-text translation popup anchored to the selection while scrolling in the
  prose and EPUB readers.
- Prevented a crash when moving between chapters in the serialized-prose reader.
- Matched the EPUB reading-progress footer to the reader background.

## [1.6.1] - 2026-07-29

### 🌟 Highlights

Continuous serialized-prose reading is restored to its previous native behavior.

### 🐛 Fixed

- Restored the native renderer for continuous serialized-prose reading after regressions in the
  WebView-based renderer.

## [1.6.0] - 2026-07-29

### 🌟 Highlights

Translate selected book text with your choice of Android, offline, or self-hosted engines, and
prepare upcoming chapters before you reach them.

### ✨ Added

- Profile-specific translation settings with a playground, explicit engine and target-language
  controls, and support for Android System Translation, Offline Translator, and configurable
  LibreTranslate servers.
- Automatic selected-text translation for serialized prose and reflowable EPUB readers, with
  independent per-reader toggles and inline engine and language controls.
- An optional **Prepare next chapter** setting that begins loading the next manga or
  serialized-prose chapter at 75% progress and preloads the first manga images.

### 🐛 Fixed

- Kept paginated serialized prose within the visible page bounds.
- Marked the final serialized-prose chapter complete after reaching its end.

## [1.5.1] - 2026-07-27

### Added

- Added richer serialized-prose rendering for thematic breaks, nested and ordered lists,
  preformatted passages, figures, tables, links, disclosures, and meaning-bearing styles.
- Added offline support for prose images and custom fonts.

### Changed

- Changed manga and prose readers to load adjacent chapters only when their transition is
  reached, keeping loading failures and retry inline.

### Improved

- Improved remote `BOOK` resource safety by rejecting HTTPS downgrade redirects and
  preventing credentials or extension headers from leaking across origins.

### Fixed

- Reopened completed or already-read `BOOK` chapters from the beginning instead of restoring
  stale partial positions.
- Kept prose and EPUB content clear of the reading-progress footer and system navigation area.

## [1.5.0] - 2026-07-26

### Added

- Added search to long source-filter option lists and source-provided filter suggestions for
  compatible extensions.
- Added BOOK source migration with reading-progress transfer and compatible location restoration.
- Added WebView, browser, and share actions for active book chapters and anime episodes when
  supported by their source.

### Changed

- Changed BOOK entry terminology from items to chapters throughout the app.

### Removed

- Removed Katari-specific legacy source API contracts

### Fixed

- Restored precise prose-reader progress after reopening a chapter.

### Other

- Updated Entry SDK to 2.3.0 with `EntryFilter.Autocomplete` for source-defined filter
  suggestions.

## [1.4.3] - 2026-07-25

### Fixed

- Hidden the migrate button in duplicate-entry dialogs when the target entry does not
  support migration.
- Preserved prose-reader scroll position when crossing chapter boundaries.

## [1.4.2] - 2026-07-25

### Added

- Added double-tap seeking to the left and right sides of immersive anime videos.

### Improved

- Improved prose-book reader responsiveness.

### Fixed

- Showed a buffering indicator during immersive anime video playback.
- Reset immersive anime video audio to muted when restarting the app.

## [1.4.1] - 2026-07-24

### Fixed

- Kept libraries, updates, history, hidden sources, and history actions isolated to the
  active profile when switching profiles.
- Automatically dissolved merge groups after they were reduced to one entry.

## [1.4.0] - 2026-07-24

### Added

- Added Reset controls to reader and player settings that clear active-profile values and
  current-entry overrides without affecting progress, bookmarks, or downloads.
- Added merging for `BOOK` entries, including grouped library, update, history, and progress
  handling. (by @pa2x2) ([#6](https://github.com/pa2x2/katari/pull/6))

### Removed

- Removed the prose reader's paragraph-spacing setting; prose now uses standard paragraph
  spacing.

### Fixed

- Applied manga reader layout and orientation changes immediately.
- Kept the app unlocked after successfully authenticating to switch to a protected profile.

## [1.3.2] - 2026-07-20

### Added

- Added a prose-reader option to render content in the display cutout area.

### Improved

- Made prose-reader chapter transitions clearer by emphasizing the next chapter.

## [1.3.1] - 2026-07-18

### Improved

- Avoided repeated full storage scans when initializing and managing BOOK downloads.

### Fixed

- Kept reading progress current while scrolling through continuous prose.
- Reset partial reading or watching progress consistently when marking manga chapters,
  anime episodes, or book chapters as unread.

## [1.3.0] - 2026-07-17

### Added

- Added source-provided related entries to entry screens for compatible Entry Source and
  Keiyoushi manga extensions.
  (by @pa2x2) ([#2](https://github.com/pa2x2/katari/pull/2))
- Added individual, bulk, and automatic BOOK downloads with Downloads-screen management,
  offline reading, and post-read cleanup preferences.
  (by @pa2x2) ([#3](https://github.com/pa2x2/katari/pull/3))

### Removed

- Removed unused advertising ID, AdServices attribution, and Play install-referrer
  permissions from telemetry-enabled builds.

### Fixed

- Prevented the final line of paginated web-novel pages from extending beyond the page.
- Preserved selected library entries while bulk and profile actions were dispatched, and
  applied category state and changes to every merged member.
  (by @pa2x2) ([#5](https://github.com/pa2x2/katari/pull/5))
- Kept long anime download-option lists scrollable while leaving the dialog actions
  accessible.

### Other

- Updated Entry SDK to 2.2.0 with the optional `RelatedEntriesSource` capability (by @pa2x2) ([#2](https://github.com/pa2x2/katari/pull/2))

## [1.2.2] - 2026-07-16

### Fixed

- Fixed a release-build crash when opening books from compatible Entry Source extensions.
  (by @pa2x2)

## [1.2.1] - 2026-07-16

### Added

- Added content-type filters to source and extension browsing, including an option to
  show items whose content type is not specified. (by @pa2x2)

### Fixed

- Restored memo data when importing Mihon backups and preserved display names from older
  Katari backups. (by @pa2x2)
- Prevented the initial library synchronization from showing newly added entries as
  updates. (by @pa2x2)

## [1.2.0] - 2026-07-16

### Added

- Added `BOOK` entries from compatible Entry Source extensions. (by @pa2x2)
- Added built-in readers for unprotected reflowable EPUBs and serialized HTML prose (by @pa2x2)

### Changed

- Grouped reader and player settings by viewer, with profile-specific book reader
  defaults, per-book layout overrides, and backup coverage for positions and overrides. (by @pa2x2)

### Improved

- Hid download, bookmark, tracking, merge, and migration controls, along with
  missing-number warnings, when unsupported by an entry type.
  (by @pa2x2)

### Other

- Updated Entry SDK to 2.1.0 with `BOOK` content and resource contracts for extension
  developers. (by @pa2x2)
- Added compatibility with manga extensions built for Keiyoushi extension API 1.6.
  (by @pa2x2)

## [1.1.0] - 2026-07-14

### Added

- Added immersive browsing for supported source catalogues, with full-screen media,
  swipe navigation, details, library actions, refresh, and position retention.
- Added configurable long-press action priorities for catalogue entries, including
  profile defaults and per-source overrides.
- Added manga and anime type indicators to source and extension listings when metadata
  is available.

### Changed

- Changed automatic backup filenames to use the `katari_...tachibk` naming format.

### Improved

- Added pull-to-refresh for catalogues and dismissed new-item indicators while scrolling
  toward newer results.

### Fixed

- Preserved episode progress and read state more reliably when source synchronization
  changes episode URLs or numbering.
- Restored tracker sign-in callbacks for Bangumi, MangaBaka, and Shikimori.

### Other

- Added the optional `SourceMetadata` capability to Entry SDK 2.0.1 so extensions can
  advertise the entry types they may supply.

## [1.0.2] - 2026-07-13

### Changed

- Replaced regular feed chips with a current-feed picker and added source labels plus
  add and manage actions to feed pickers in regular and immersive views.

### Improved

- Kept chronological feed refreshes continuous across multi-page gaps while preserving
  item order and scroll position, with loading progress and a shortcut to the newest results.

## [1.0.1] - 2026-07-12

### Improved

- Refreshed high-volume custom feeds without prolonged loading and showed the available
  new items promptly.
- Made new-item indicators clearer and available in immersive feeds.
- Unmuted immersive-feed video when raising the device volume.

### Fixed

- Restored removal of entries from the library.

## [1.0.0] - 2026-07-11

Based on [Mihon v0.20.1](https://github.com/mihonapp/mihon/releases/tag/v0.20.1).

### Added

- A unified library, browse experience, updates feed, and history for reading and
  watching content.
- Profiles with separate libraries, categories, appearance, tracking, and
  preferences.
- Video playback with streaming, subtitles, quality selection, progress tracking,
  and offline downloads.
- Custom discovery feeds, including immersive layouts and media previews.
- Merged entries for keeping the same title from multiple sources together.
- The Entry Source API and extension SDK for sources that provide image or
  playback media.
- Compatibility support for selected Mihon extension API families.

### Changed

- Rebranded the application as Katari with its own package identity, visual
  identity, release pipeline, and documentation.

[Unreleased]: https://github.com/pa2x2/katari/compare/v1.7.0...upcoming
[1.7.0]: https://github.com/pa2x2/katari/releases/tag/v1.7.0
[1.6.2]: https://github.com/pa2x2/katari/releases/tag/v1.6.2
[1.6.1]: https://github.com/pa2x2/katari/releases/tag/v1.6.1
[1.6.0]: https://github.com/pa2x2/katari/releases/tag/v1.6.0
[1.5.1]: https://github.com/pa2x2/katari/releases/tag/v1.5.1
[1.5.0]: https://github.com/pa2x2/katari/releases/tag/v1.5.0
[1.4.3]: https://github.com/pa2x2/katari/releases/tag/v1.4.3
[1.4.2]: https://github.com/pa2x2/katari/releases/tag/v1.4.2
[1.4.1]: https://github.com/pa2x2/katari/releases/tag/v1.4.1
[1.4.0]: https://github.com/pa2x2/katari/releases/tag/v1.4.0
[1.3.2]: https://github.com/pa2x2/katari/releases/tag/v1.3.2
[1.3.1]: https://github.com/pa2x2/katari/releases/tag/v1.3.1
[1.3.0]: https://github.com/pa2x2/katari/releases/tag/v1.3.0
[1.2.2]: https://github.com/pa2x2/katari/releases/tag/v1.2.2
[1.2.1]: https://github.com/pa2x2/katari/releases/tag/v1.2.1
[1.2.0]: https://github.com/pa2x2/katari/releases/tag/v1.2.0
[1.1.0]: https://github.com/pa2x2/katari/releases/tag/v1.1.0
[1.0.2]: https://github.com/pa2x2/katari/releases/tag/v1.0.2
[1.0.1]: https://github.com/pa2x2/katari/releases/tag/v1.0.1
[1.0.0]: https://github.com/pa2x2/katari/releases/tag/v1.0.0
