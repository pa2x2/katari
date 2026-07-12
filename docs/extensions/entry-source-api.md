# Understanding `entry-source-api`

The Entry API separates three questions:

1. What appears in a catalogue?
2. What can the user open from an entry?
3. Which renderer should consume the resolved media?

## Catalogue lifecycle

Every `UnifiedSource` implements the same suspending operations:

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

Return `hasNextPage = true` only when requesting the next page can produce more results.

`EntryHttpSource` adds a shared network client, headers, URL helpers, catalogue metadata, and a stable-ID algorithm. `EntryImageHttpSource` adds image requests and chapter URL support.

## Entries

Create catalogue results with `SEntry.create()`:

```kotlin
val entry = SEntry.create().apply {
    url = "/title/example"
    title = "Example"
    thumbnailUrl = "https://example.com/cover.jpg"
    status = SEntry.ONGOING
    type = EntryType.MANGA
}
```

The entry URL is its identity inside a source. Keep it stable and prefer a relative URL when extending `EntryHttpSource`.

`EntryType` belongs to each entry rather than to the extension or source, so a catalogue can return multiple supported types.

When enriching an existing entry, copy it rather than discarding fields already discovered by the catalogue:

```kotlin
return entry.copy().apply {
    initialized = true
    description = parsedDescription
    genre = parsedGenres
    type = parsedType
}
```

## Chapters

`SEntryChapter` represents an openable unit. The name can be a chapter, episode, movie, gallery, or another user-facing label:

```kotlin
val chapter = SEntryChapter.create().apply {
    url = "/title/example/chapter-1"
    name = "Chapter 1"
    chapterNumber = 1.0
    dateUpload = uploadTimeMillis
}
```

Keep chapter URLs stable after release. Katari uses source, entry, and chapter identity to preserve library and history state.

## Image media

Image sources return `EntryMedia.ImagePages`:

```kotlin
return EntryMedia.ImagePages(
    imageUrls.mapIndexed { index, imageUrl ->
        EntryImagePage(
            index = index,
            url = imageUrl,
            imageUrl = imageUrl,
        )
    },
)
```

Use `EntryImageHttpSource` when an HTTP source can compose the default image behavior from `EntryImageSource` with its chapter and request helpers. Override `getImageUrl()` or `imageRequest()` when pages require a separate resolution request or special headers.

## Playback media

Video sources return a playback descriptor:

```kotlin
return EntryMedia.Playback(
    PlaybackDescriptor(
        selection = selection,
        streams = listOf(
            VideoStream(
                request = VideoRequest(
                    url = streamUrl,
                    headers = mapOf("Referer" to "$baseUrl/"),
                ),
                label = "1080p",
                type = VideoStreamType.HLS,
            ),
        ),
    ),
)
```

`PlaybackDescriptor` can also advertise dub and source-quality options. If a requested option is unavailable, return the actual fallback in `descriptor.selection`. Implement `SubtitleSource` when subtitle tracks are resolved separately from the streams.

## Filters

Return `EntryFilterList` from `getFilterList()` and inspect the same filter types in `getSearchContent()`:

```kotlin
private class SortFilter : EntryFilter.Select<String>(
    name = "Sort",
    values = arrayOf("Popular", "Newest"),
)

override fun getFilterList() = EntryFilterList(SortFilter())
```

Filter state is mutable. Treat the list received by `getSearchContent()` as the user's current selection rather than retaining an older copy.

## Optional capabilities

A source can opt into additional behavior by implementing focused interfaces:

- `ConfigurableSource` adds an Android preference screen scoped to the source.
- `SubtitleSource` resolves external video subtitles.
- `EntryPreviewSource` supplies static title preview images. Katari currently consumes this capability for anime entries.
- `ResolvableSource` resolves supported external URLs.
- `EntryItemOrientationProvider` chooses catalogue item orientation.
- `EmptyChapterListSource`, `IncrementalChapterSource`, and `ChapterNumberRecognitionSource` describe chapter-list behavior.
- `UnmeteredSource` opts the source out of Katari's metered-source warnings during library updates. It does not change how Android or the network accounts for data.

Prefer capabilities over checking concrete source classes. This lets Katari add new content behavior without creating another parallel extension API.

## Stable source identity

`EntryHttpSource` derives its ID from `name`, `lang`, and `versionId` unless the source overrides `id`. Changing any of those inputs changes the generated ID. Once an extension has users, preserve its source ID or provide an intentional migration path.

The complete public contracts are available in [`entry-source-api/src`](https://github.com/katariapp/katari/tree/main/entry-source-api/src).
