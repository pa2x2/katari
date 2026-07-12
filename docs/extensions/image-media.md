# Image media cookbook

Use `EntryImageHttpSource` when an openable unit resolves to ordered image pages. It implements `EntryImageSource` and supplies the default client, headers, chapter URL, and image request behavior.

## Return direct image URLs

When the chapter response already contains final image URLs, set both `url` and `imageUrl`:

```kotlin
override suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): EntryMedia {
    val request = GET(getChapterUrl(chapter), headers)
    val pages = client.newCall(request).awaitSuccess().use { response ->
        response.asJsoup().select("main.reader img").mapIndexed { index, image ->
            val imageUrl = image.absUrl("data-src").ifEmpty { image.absUrl("src") }
            EntryImagePage(
                index = index,
                url = imageUrl,
                imageUrl = imageUrl,
            )
        }
    }
    return EntryMedia.ImagePages(pages)
}
```

Page indices must be unique and ordered from zero. Do not include advertisements, placeholders, or lazy-loading sentinel images.

## Resolve images lazily

Some sites expose an intermediate page URL and reveal the image only when it is opened. Leave `imageUrl` unset and override `getImageUrl()`:

```kotlin
override suspend fun getImageUrl(page: EntryImagePage): String {
    return client.newCall(GET(page.url, headers)).awaitSuccess().use { response ->
        response.asJsoup().selectFirst("img#page")!!.absUrl("src")
    }
}
```

Use lazy resolution for genuinely expiring or indirect URLs. If the final URL is already available, returning it in `EntryImagePage.imageUrl` avoids an extra request per page.

## Add image-specific headers

Override `imageRequest()` when the image host validates a referrer or needs headers that catalogue requests do not:

```kotlin
override fun imageRequest(page: EntryImagePage, imageUrl: String): Request =
    GET(
        imageUrl,
        headers.newBuilder()
            .set("Referer", "$baseUrl/")
            .build(),
    )
```

The example uses a provider-wide referrer. When a host requires a chapter-specific referrer, put enough non-secret context in `EntryImagePage.url` and derive it from that stable page data. Do not keep mutable current-chapter state: several chapters can load concurrently.

## Operational guidance

- Return the provider's reading order; Katari handles reader direction separately.
- Keep chapter URLs stable because they anchor history and download state.
- Prefer HTTPS and preserve signed query parameters.
- Do not download image bytes in `getMedia()`. Return descriptors and let `EntryImageSource` load pages on demand.
- Throw a useful error when a chapter resolves to no pages unexpectedly. An empty successful result looks like valid empty content.
- Test duplicate indices, missing lazy-loaded attributes, relative URLs, redirects, and image-host header requirements.

For catalogue and chapter-page requests, continue with [HTTP and parsing](./http-and-parsing.md).
