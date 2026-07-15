# Changelog

All notable Katari-specific changes will be documented in this file.

Katari is built on [Mihon](https://github.com/mihonapp/mihon). Changes inherited
from Mihon are documented in the [Mihon changelog](https://github.com/mihonapp/mihon/blob/main/CHANGELOG.md);
this file covers the features and behavior that distinguish Katari.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[Unreleased]: https://github.com/katariapp/katari/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/katariapp/katari/releases/tag/v1.1.0
[1.0.2]: https://github.com/katariapp/katari/releases/tag/v1.0.2
[1.0.1]: https://github.com/katariapp/katari/releases/tag/v1.0.1
[1.0.0]: https://github.com/katariapp/katari/releases/tag/v1.0.0
