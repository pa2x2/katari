# Entry SDK changelog

This changelog covers the public `entry-source-api` contract. It is separate from Katari app release notes and from individual extension revisions.

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
