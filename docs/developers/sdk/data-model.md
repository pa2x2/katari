# Entry SDK data model

The SDK models three stages: an entry discovered in a catalogue, an openable child item belonging to that entry, and the media resolved when that child is opened.

## Entries

Create source entries with `SEntry.create()`:

```kotlin
val entry = SEntry.create().apply {
    url = "/title/example"
    title = "Example"
    thumbnailUrl = "https://example.com/cover.jpg"
    status = SEntry.ONGOING
    type = EntryType.MANGA
}
```

Important properties include:

| Property | Contract |
| --- | --- |
| `url` | Stable identity within the source. Prefer a domain-free URL when the path is stable. |
| `title` | User-visible title. It must not be used as identity. |
| `type` | Supported content type for this particular entry. |
| `status` | One of the `SEntry` status constants, or `UNKNOWN`. |
| `initialized` | Whether the details request has populated the entry. |
| `updateStrategy` | Optional instruction controlling future metadata updates. |
| `memo` | JSON metadata that must survive source lifecycle calls but is not directly shown to users. |

Catalogue results should set `type` as soon as it is known. When enriching an entry, copy it so fields already discovered by the catalogue survive:

```kotlin
return entry.copy().apply {
    initialized = true
    description = parsedDescription
    genre = parsedGenres
    type = parsedType
}
```

Use `memo` sparingly. Provider-specific values placed there become persisted compatibility state. Resolve temporary tokens, signed media URLs, and other expiring values at request time instead.

## Child items

`SEntryChapter` is the historical API name for an openable unit. Depending on the entry type it may represent a chapter, episode, movie, publication, volume, gallery, or another user-facing unit:

```kotlin
val child = SEntryChapter.create().apply {
    url = "/title/example/chapter-1"
    name = "Chapter 1"
    chapterNumber = 1.0
    dateUpload = uploadTimeMillis
}
```

The child `url` is persistent identity. Changing it after release can disconnect history, downloads, and existing library state. `chapterNumber = -1.0` represents an unknown number; sources may opt into host-side recognition through `ChapterNumberRecognitionSource`.

## Pagination

Catalogue methods return `EntryPageResult<T>`:

```kotlin
return EntryPageResult(
    items = parsedEntries,
    hasNextPage = nextPageUrl != null,
)
```

Return `hasNextPage = true` only when another request can produce results. Prefer a provider cursor or explicit next link over guessing from the current item count.

## Filters

`EntryFilterList` contains mutable `EntryFilter` instances. Available filter shapes include headers, separators, selections, text, check boxes, tri-state values, groups, and sortable selections.

Create a new filter list when Katari requests it, then inspect the list passed to `getSearchContent()`:

```kotlin
private class SortFilter : EntryFilter.Select<String>(
    name = "Sort",
    values = arrayOf("Popular", "Newest"),
)

override fun getFilterList() = EntryFilterList(SortFilter())

override suspend fun getSearchContent(
    page: Int,
    query: String,
    filters: EntryFilterList,
): EntryPageResult<SEntry> {
    val sort = filters.filterIsInstance<SortFilter>().first().state
    return search(page, query, sort)
}
```

Do not retain an older filter list as request state. The argument contains the user's current selections.

## Media

`getMedia()` returns a sealed `EntryMedia` payload:

- `EntryMedia.ImagePages` contains ordered `EntryImagePage` descriptors for the reader.
- `EntryMedia.Playback` contains a `PlaybackDescriptor` for the player.
- `EntryMedia.Book` contains a processor-selection descriptor and source resource catalogue for a book reader.

Image page indices must be unique and ordered from zero. A page can carry a direct `imageUrl`, or an intermediate `url` that `EntryImageSource.getImageUrl()` resolves lazily.

A playback descriptor reports the streams and the selection actually resolved. Streams and subtitles carry their own `VideoRequest` headers because player requests do not automatically inherit the source's catalogue headers.

### Book media

Book media keeps stable publication/resource identity separate from current access locations:

```kotlin
return EntryMedia.Book(
    descriptor = BookContentDescriptor(
        format = "application/epub+zip",
    ),
    publicationRevision = publication.revision,
    catalog = BookResourceCatalog(
        resources = listOf(
            BookSourceResource(
                id = "epub",
                title = "EPUB",
                mediaType = "application/epub+zip",
                size = publication.epubSize,
                revision = publication.epubRevision,
                availability = BookResourceAvailability.AVAILABLE,
                location = BookResourceLocation.RemoteRequest(
                    url = publication.epubUrl,
                    headers = headers.toMap(),
                ),
            ),
        ),
        coverage = BookCatalogCoverage.COMPLETE,
    ),
    initialResourceId = "epub",
)
```

Resource IDs are publication-scoped persistence keys. Do not derive them from expiring URLs. Revisions describe content changes and allow Katari to reconcile caches and progress. Keep large content out of inline locations; use a remote request, source child, local URI, or app reference instead.

`BookContentDescriptor` and the processor-normalized publication/locator models come from the transitive `book-api` artifact. Source-side catalogues and locations remain in `entry-source-api`. See [Book API architecture](./book-api.md) for why the boundary is split.

Use the [image media](../../extensions/image-media.md), [playback media](../../extensions/playback-media.md), and [book media](../../extensions/book-media.md) cookbooks for complete implementations.
