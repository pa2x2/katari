# Entry SDK overview

The Entry SDK is Katari's public extension-authoring API. It separates catalogue metadata from the media that Katari opens, allowing one source to expose entries of different supported types without maintaining parallel source APIs.

New extensions depend on `entry-source-api` as `compileOnly`. Katari supplies the API and runtime dependencies when it loads the extension. See [create your first extension](../../extensions/getting-started.md) for the Gradle dependency and manifest setup.

## Core lifecycle

An extension exposes either one `UnifiedSource` implementation or an `EntrySourceFactory` that creates one or more sources:

```text
EntrySourceFactory
    └── UnifiedSource
        ├── catalogue pages
        ├── entry details
        ├── child items
        └── resolved image, playback, or book media
```

Every source provides the same core operations:

```kotlin
suspend fun getPopularContent(page: Int): EntryPageResult<SEntry>
suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry>
suspend fun getSearchContent(
    page: Int,
    query: String,
    filters: EntryFilterList,
): EntryPageResult<SEntry>
suspend fun getContentDetails(entry: SEntry): SEntry
suspend fun getChapterList(entry: SEntry): List<SEntryChapter>
suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection = PlaybackSelection(),
): EntryMedia
```

The normal flow is:

1. A catalogue call returns lightweight entries with stable identities.
2. `getContentDetails()` enriches the selected entry without discarding known fields.
3. `getChapterList()` returns the openable child items for that entry.
4. `getMedia()` resolves the selected child item into a reader or player payload.

Katari can invoke catalogue and media operations concurrently. A source must not keep mutable "current entry" or "current chapter" state between calls.

## Choose a source base

- Extend `EntryImageHttpSource` for HTTP sources that resolve ordered image pages.
- Extend `EntryHttpSource` for HTTP sources that resolve playback, book, or another non-image payload.
- Implement `UnifiedSource` directly when the provided HTTP client and URL behavior are not appropriate.
- Implement `EntrySourceFactory` when one extension exposes multiple sources.

`EntryHttpSource` supplies Katari's shared network client, default headers, URL helpers, catalogue metadata, WebView URLs, and a stable source-ID algorithm. Its `name`, `lang`, and `versionId` inputs become persistent identity once users have installed the source.

## Core and optional contracts

`UnifiedSource` is the required lifecycle contract. `EntryCatalogueSource` adds language and catalogue presentation metadata. Focused capability interfaces opt a source into additional behavior or descriptive metadata such as supported entry types, image loading, subtitles, previews, related-entry discovery, preferences, URI resolution, or incremental child refreshes.

Prefer capability checks over concrete-class checks. This allows Katari to add behavior without creating another parallel source hierarchy. See [capabilities](./capabilities.md) for the full matrix.

## Type and media are separate

Each `SEntry` declares its own `EntryType`; the extension or source is not permanently classified. `getMedia()` returns the concrete payload Katari should render for a child item.

This separation allows a factory—and, where appropriate, a catalogue—to expose several supported content types. Read [content types](./content-types.md) before adding type-dependent source behavior.

BOOK adds another separation inside its media path: sources describe resources, Katari owns access to those resources, and a format processor interprets them and supplies its reader. The shared data-only boundary lives in a separate artifact so processors do not depend on the extension source lifecycle. Read [Book API architecture](./book-api.md) before implementing book media.

## Reference and compatibility

The [data model](./data-model.md) documents values and identity rules. The generated [Entry Source API reference](api/index.html){target="_self"} and [Book API reference](api/book/index.html){target="_self"} list the public Kotlin surfaces.

Before adopting another release, read [compatibility and versioning](./versioning.md) and the [SDK changelog](./changelog.md). Compiling against a new symbol also requires a Katari app release that supplies that symbol at runtime.
