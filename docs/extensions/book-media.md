# Book media cookbook

Use `EntryHttpSource` for catalogue entries that open as books. A book source lists independently openable children and describes how Katari can retrieve the selected child's resources. It does not implement a reader; Katari selects a compatible book processor, and that processor supplies the reader UI.

Katari includes separate built-in processors for reflowable EPUB publications and normalized HTML prose chapters. The source-facing contracts remain format-neutral so other book formats and readers can be added without changing `UnifiedSource`.

## Return book entries

Set `EntryType.BOOK` in catalogue results as soon as the provider identifies the item as a book:

```kotlin
SEntry.create().apply {
    url = "/books/${item.id}"
    title = item.title
    author = item.author
    thumbnailUrl = item.coverUrl
    status = SEntry.COMPLETED
    type = EntryType.BOOK
}
```

`SEntry.url` remains the stable source identity. Do not use a download URL, signed URL, or selected rendition as the entry identity.

Advertise BOOK as source metadata when the source is known to provide it:

```kotlin
class ExampleBookSource : EntryHttpSource(), SourceMetadata {
    override val supportedEntryTypes = setOf(EntryType.BOOK)
}
```

::: info

This metadata lets Katari describe the source before loading its catalogue; it does not replace `SEntry.type`. If the extension repository publishes source metadata, keep its `supportedEntryTypes` value aligned with the runtime source. See [publishing and maintenance](./publishing.md#publish-source-discovery-metadata).

:::

## Model openable items

`SEntryChapter` is the historical SDK name for every openable child item. For a single-file EPUB, return one child representing the publication:

```kotlin
override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
    val publication = fetchPublication(entry.url)
    if (publication.epub == null) return emptyList()

    return listOf(
        SEntryChapter.create().apply {
            url = "/books/${publication.id}/epub"
            name = "EPUB"
            chapterNumber = 1.0
            dateUpload = publication.updatedAt
        },
    )
}
```

Use one child per independently openable source item. Do not project an EPUB's internal spine or table of contents into `getChapterList()`; the EPUB processor discovers and navigates that structure after opening the archive. For a serialized web novel, return one child per provider chapter because those chapters are independently fetched, tracked, and opened.

The child URL is persistent identity for consumption and history. Keep expiring acquisition URLs and authorization tokens out of it. Resolve current resource access inside `getMedia()`.

## Return a remote EPUB

The built-in EPUB processor recognizes the `application/epub+zip` format. It accepts an absent profile or the `reflowable` profile, requires unprotected content, and rejects fixed-layout EPUBs.

```kotlin
private const val EPUB_MEDIA_TYPE = "application/epub+zip"
private const val EPUB_RESOURCE_ID = "epub"

override suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): EntryMedia {
    val epub = resolveCurrentEpub(chapter)
    val location = BookResourceLocation.RemoteRequest(
        url = epub.downloadUrl,
        headers = epub.downloadHeaders,
    )

    return EntryMedia.Book(
        descriptor = BookContentDescriptor(
            format = EPUB_MEDIA_TYPE,
            protection = "none",
        ),
        publicationRevision = epub.publicationRevision,
        catalog = BookResourceCatalog(
            resources = listOf(
                BookSourceResource(
                    id = EPUB_RESOURCE_ID,
                    title = "EPUB",
                    order = 0,
                    mediaType = EPUB_MEDIA_TYPE,
                    size = epub.contentLength,
                    revision = epub.resourceRevision,
                    availability = BookResourceAvailability.AVAILABLE,
                    location = location,
                ),
            ),
            revision = epub.catalogRevision,
            coverage = BookCatalogCoverage.COMPLETE,
        ),
        initialResourceId = EPUB_RESOURCE_ID,
        initialResourceLocation = location,
    )
}
```

`resolveCurrentEpub()` is a source helper that obtains the current acquisition URL, headers, size, and revisions from the provider. The processor materializes and validates the EPUB archive; the extension should not download or unzip it inside `getMedia()`.

Use the exact media type as the descriptor format. `epub`, `epub3`, and reader implementation names are not compatible substitutes. Omit `profile` when the provider does not state a narrower profile; the processor validates the EPUB layout while opening it. Set `profile = "reflowable"` only when the source can identify that profile authoritatively. `protection` defaults to `none`, so return this descriptor only for content known to be unprotected. Do not label fixed-layout or protected content as `reflowable` or `none` merely to select the built-in processor.

## Return a serialized prose chapter

The built-in prose processor recognizes `text/html` with the `prose-chapter` profile. This profile represents one passive, independently openable prose document. It does not represent an entire novel, an arbitrary website, or an EPUB-like spine.

Return one `SEntryChapter` per provider chapter:

```kotlin
override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
    return fetchProviderChapters(entry).mapIndexed { index, item ->
        SEntryChapter.create().apply {
            url = "/novels/${entry.url}/chapters/${item.stableId}"
            name = item.title
            chapterNumber = (index + 1).toDouble()
            dateUpload = item.publishedAt
        }
    }
}
```

Resolve only the selected chapter in `getMedia()`:

```kotlin
private const val PROSE_FORMAT = "text/html"
private const val PROSE_PROFILE = "prose-chapter"

override suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): EntryMedia {
    val resolved = fetchNormalizedChapter(chapter.url)
    val location = BookResourceLocation.InlineText(
        text = resolved.bodyHtml,
        mediaType = PROSE_FORMAT,
    )
    val resource = BookSourceResource(
        id = resolved.stableId,
        title = chapter.name,
        order = 0,
        mediaType = PROSE_FORMAT,
        revision = resolved.revision,
        availability = BookResourceAvailability.AVAILABLE,
        location = location,
    )

    return EntryMedia.Book(
        descriptor = BookContentDescriptor(
            format = PROSE_FORMAT,
            profile = PROSE_PROFILE,
            protection = "none",
        ),
        catalog = BookResourceCatalog(
            resources = listOf(resource),
            coverage = BookCatalogCoverage.PARTIAL,
        ),
        initialResourceId = resource.id,
        initialResourceLocation = location,
    )
}
```

The result contains one resource even when the entry has thousands of chapters. Katari already stores the sibling `SEntryChapter` records and uses them for previous/next navigation. The prose processor renders only the selected resource and tracks progression against that chapter.

`fetchNormalizedChapter()` must return the prose body rather than the provider's complete website shell. Preserve meaningful structure such as headings, paragraphs, emphasis, quotations, lists, tables, and same-document anchors. The built-in processor applies its own sanitization and disables active or remote web content, but extensions should still avoid returning scripts, forms, embedded media, styles, or navigation chrome.

If the selected chapter is locked, removed, or otherwise inaccessible, return the same one-resource shape with the accurate `BookResourceAvailability` and no readable location. Do not return a preview while marking the resource `AVAILABLE`.

## Keep identity separate from access

Book progress depends on stable publication and resource identity:

- `SEntry.url` identifies the source entry.
- `SEntryChapter.url` identifies the openable source child.
- `publicationKeyOverride` distinguishes multiple logical publications only when one entry genuinely contains them.
- `BookSourceResource.id` identifies a resource within that publication. Keep `epub`, a provider rendition ID, or another stable key across URL changes.
- `BookResourceLocation` describes current access and may change on every resolution.

For a media result with one catalog resource, Katari can infer the primary resource. Setting `initialResourceId` explicitly is clearer and is required when the catalog contains more than one candidate. `initialResourceLocation` supplies the location resolved for the selected child; keep the matching location in the catalog resource so later resource lookup remains self-contained.

## Report revisions and catalog coverage

Revisions allow Katari to distinguish updated content from a changed access URL:

- `publicationRevision` identifies the logical publication revision.
- `BookResourceCatalog.revision` identifies the returned catalog snapshot.
- `BookSourceResource.revision` identifies the bytes or text of that resource.

Use authoritative provider values such as a content revision, ETag, or `Last-Modified` value. Leave a revision `null` when the provider supplies no stable value; do not generate one from the current time. A signed URL refresh is not a content revision.

Use `BookCatalogCoverage.COMPLETE` when the result describes the complete known publication, `ONGOING` when new resources are expected, and `PARTIAL` when the source intentionally returns only part of the catalog. A standalone prose chapter normally uses `PARTIAL` because sibling chapters remain source children rather than resources in the selected media result. Use `UNKNOWN` only when no stronger statement is possible.

## Provide resource access safely

`BookResourceLocation.RemoteRequest` must contain an absolute HTTP or HTTPS URL and every header required by the resource host. Catalogue headers are not automatically copied into the resource request. Include only required values such as `User-Agent`, `Accept`, `Referer`, `Origin`, or authorization.

Resolve short-lived URLs and credentials in `getMedia()`. Do not put secrets in entry URLs, child URLs, titles, errors, revisions, or logs. Preserve signed query parameters in the remote request.

Set `size` when the provider gives a trustworthy content length. Katari uses resource metadata to enforce materialization limits before opening large content. Do not issue a full download merely to calculate the size.

Other closed location variants support bounded inline content, Katari-owned content URIs, and indirection through another stable source child. `AppReference` is reserved for identifiers issued by a future Katari-owned retrieval flow; current builds report it as `UNSUPPORTED_APP_ACCESS`, so extensions must not construct one. A processor never receives the source object or executable source callbacks.

## Describe unavailable content truthfully

Use `BookResourceAvailability` to report known access state. Mark a resource `AVAILABLE` only when Katari can retrieve it with the supplied location. Other values distinguish authentication, purchase, region, removal, and unsupported app-access conditions.

The descriptor must describe the actual content even when Katari has no compatible processor. Katari then shows the dedicated unsupported-content screen. A source must not disguise another format as EPUB or protected content as unprotected to avoid that fallback.

## Operational guidance

- Keep resource IDs unique within a publication and catalog sizes within the SDK limit.
- Return exactly one primary EPUB archive for the built-in EPUB processor.
- Return exactly one primary HTML resource for the built-in prose chapter processor.
- Prefer HTTPS and resolve redirects or expiring acquisitions at open time.
- Keep `getMedia()` safe for concurrent calls; do not store a mutable current entry or child.
- Preserve coroutine cancellation when fetching metadata.
- Test missing renditions, expired URLs, required headers, unknown content length, changed revisions, duplicate resource IDs, unsupported profiles, and unavailable resources.
- Test the extension against the matching Katari runtime with [local SDK development](../developers/sdk/local-development.md).

See [Book API architecture](../developers/sdk/book-api.md) for the source/processor boundary and [content types](../developers/sdk/content-types.md#book-entries) for the generic BOOK contract.
