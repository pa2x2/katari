# Changelog

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
  handling. (by @pa2x2) ([#6](https://github.com/katariapp/katari/pull/6))

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
  (by @pa2x2) ([#2](https://github.com/katariapp/katari/pull/2))
- Added individual, bulk, and automatic BOOK downloads with Downloads-screen management,
  offline reading, and post-read cleanup preferences.
  (by @pa2x2) ([#3](https://github.com/katariapp/katari/pull/3))

### Removed

- Removed unused advertising ID, AdServices attribution, and Play install-referrer
  permissions from telemetry-enabled builds.

### Fixed

- Prevented the final line of paginated web-novel pages from extending beyond the page.
- Preserved selected library entries while bulk and profile actions were dispatched, and
  applied category state and changes to every merged member.
  (by @pa2x2) ([#5](https://github.com/katariapp/katari/pull/5))
- Kept long anime download-option lists scrollable while leaving the dialog actions
  accessible.

### Other

- Updated Entry SDK to 2.2.0 with the optional `RelatedEntriesSource` capability (by @pa2x2) ([#2](https://github.com/katariapp/katari/pull/2))

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

[Unreleased]: https://github.com/katariapp/katari/compare/v1.4.1...upcoming
[1.4.1]: https://github.com/katariapp/katari/releases/tag/v1.4.1
[1.4.0]: https://github.com/katariapp/katari/releases/tag/v1.4.0
[1.3.2]: https://github.com/katariapp/katari/releases/tag/v1.3.2
[1.3.1]: https://github.com/katariapp/katari/releases/tag/v1.3.1
[1.3.0]: https://github.com/katariapp/katari/releases/tag/v1.3.0
[1.2.2]: https://github.com/katariapp/katari/releases/tag/v1.2.2
[1.2.1]: https://github.com/katariapp/katari/releases/tag/v1.2.1
[1.2.0]: https://github.com/katariapp/katari/releases/tag/v1.2.0
[1.1.0]: https://github.com/katariapp/katari/releases/tag/v1.1.0
[1.0.2]: https://github.com/katariapp/katari/releases/tag/v1.0.2
[1.0.1]: https://github.com/katariapp/katari/releases/tag/v1.0.1
[1.0.0]: https://github.com/katariapp/katari/releases/tag/v1.0.0
