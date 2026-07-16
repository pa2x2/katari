# Entry SDK changelog

This changelog covers the public Entry SDK contracts in `entry-source-api` and `book-api`. It is separate from Katari app release notes and from individual extension revisions.

## `sdk-2.2.0`

Source-defined related-entry discovery release.

### Added

- Optional `RelatedEntriesSource` capability for loading entries related to a selected entry.
- Related results use normal `SEntry` values and may contain mixed entry types.

### Compatibility

- First supplied by Katari `1.3.0`.
- New loader family: `2.2`.
- Katari continues to accept Entry SDK families `2.0` and `2.1`; existing compiled extensions do not need to move to `2.2`.
- Extensions implementing `RelatedEntriesSource` must declare family `2.2` in the first two components of Android `versionName`.
- Compatible Keiyoushi sources with a concrete direct-related implementation are bridged to the capability. Katari does not substitute title search for missing related-entry support.

## `sdk-2.1.0`

BOOK source contracts and processor-neutral book data release.

### Added

- `EntryType.BOOK` and `EntryMedia.Book` for source-provided readable publications.
- The coordinated `book-api` artifact containing processor-neutral format descriptors, resource access metadata, normalized publication models, persistent locators, and structured failures.
- Source-side book resource catalogues, grouping hints, and closed data-only locations for remote requests, existing source children, bounded inline content, local content URIs, and app-owned references.

### Compatibility

- First supplied by Katari `1.2.0`.
- New loader family: `2.1`.
- Katari continues to accept Entry SDK family `2.0`; existing compiled extensions do not need to move to `2.1`.
- Extensions using BOOK APIs must declare family `2.1` in the first two components of Android `versionName`.
- `book-api` and `entry-source-api` are versioned and published together under the same `sdk-*` tag.

See [Book API architecture](./book-api.md) for the artifact boundary.

## `sdk-2.0.1`

### Added

- Optional `SourceMetadata` capability for advertising the `EntryType` values a source may supply.

## `sdk-2.0.0`

Initial public Entry SDK release.

### Added

- Type-agnostic `UnifiedSource` lifecycle using suspending catalogue, details, child-list, and media operations.
- Entry-native `SEntry`, `SEntryChapter`, pagination, filter, update-strategy, and content-type models.
- Image reader payloads through `EntryMedia.ImagePages`, `EntryImageSource`, and `EntryImageHttpSource`.
- Playback payloads through `EntryMedia.Playback`, playback selections, streams, requests, and subtitles.
- Factory, catalogue, preferences, URI resolution, WebView, home-page, preview, orientation, child-list, immersive-feed, and metered-warning capabilities.
- Entry-native HTTP, URL, and Jsoup helpers.

### Compatibility

- Loader family: `2.0`.
- New extensions should use `entry-source-api` and must not depend on the legacy `source-api` models or RxJava contracts.

See [compatibility and versioning](./versioning.md) for how future patch, minor, and major releases affect extensions.
