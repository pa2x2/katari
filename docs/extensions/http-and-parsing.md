# HTTP and parsing

`EntryHttpSource` supplies Katari's shared `OkHttpClient`, default user agent, source headers, and URL helpers. The SDK's core networking dependency supplies suspending extensions such as `awaitSuccess()`. Use the shared client instead of constructing an unrelated client so requests share Katari's cookies, DNS behavior, and network configuration.

## Build requests

Override `headersBuilder()` for headers that apply to most requests:

```kotlin
override fun headersBuilder() = super.headersBuilder()
    .add("Referer", "$baseUrl/")
    .add("Accept", "text/html,application/xhtml+xml")
```

Build a request with the SDK's request helpers and execute it with `awaitSuccess()`:

```kotlin
private fun popularRequest(page: Int) = GET(
    "$baseUrl/catalogue?page=$page",
    headers,
)

override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> =
    client.newCall(popularRequest(page)).awaitSuccess().use { response ->
        popularParse(response)
    }
```

Always close a `Response`; `use` does this even when parsing fails. `awaitSuccess()` throws for unsuccessful HTTP responses and cooperates with coroutine cancellation. Do not replace it with blocking `execute()` inside a suspending source method.

Add request-specific headers to a derived request rather than changing the shared `headers` object:

```kotlin
val request = GET(url, headers).newBuilder()
    .header("Referer", chapterPageUrl)
    .build()
```

!!! warning

    Avoid logging authentication headers, cookies, signed URLs, or response bodies containing user data.

## Parse HTML

The SDK provides `Response.asJsoup()` and small Jsoup helpers:

```kotlin
private fun popularParse(response: Response): EntryPageResult<SEntry> {
    val document = response.asJsoup()
    val entries = document.select("article.title").map { element ->
        SEntry.create().apply {
            setUrlWithoutDomain(element.selectFirst("a")!!.absUrl("href"))
            title = element.selectText("h2")!!.trim()
            thumbnailUrl = element.selectFirst("img")?.absUrl("src")
            type = EntryType.MANGA
        }
    }
    val hasNextPage = document.selectFirst("a[rel=next]") != null
    return EntryPageResult(entries, hasNextPage)
}
```

Use selectors anchored to stable semantic attributes when possible. Treat optional metadata as optional, but fail clearly when identity fields such as title or URL are absent. Silent placeholder identities can merge unrelated entries.

`setUrlWithoutDomain()` retains the path, query, and fragment. Prefer it when a provider returns absolute links so a domain change does not rewrite every stored identity. Do not remove query parameters when the provider uses them to identify content.

## Parse JSON

Read the body once, deserialize into private transport models, and map those models into the public Entry types:

```kotlin
@Serializable
private data class CatalogueItem(
    val slug: String,
    val name: String,
    val cover: String? = null,
)

private fun popularParse(response: Response): EntryPageResult<SEntry> {
    val payload = json.decodeFromString<CatalogueResponse>(response.body.string())
    return EntryPageResult(
        items = payload.items.map { item ->
            SEntry.create().apply {
                url = "/title/${item.slug}"
                title = item.name
                thumbnailUrl = item.cover
                type = EntryType.MANGA
            }
        },
        hasNextPage = payload.nextPage != null,
    )
}
```

Keep provider-specific response models out of `SEntry.memo` unless values genuinely must survive between lifecycle calls. Stored memo data becomes part of the extension's maintenance burden.

## Pagination and filters

Return `hasNextPage = true` only when another request can produce results. Prefer a server-provided cursor or next link over guessing from the number of returned items.

The filters passed to `getSearchContent()` contain the user's current mutable state. Inspect that argument for every request:

```kotlin
val sort = filters.filterIsInstance<SortFilter>().first().state
val tag = filters.filterIsInstance<TagFilter>().first().state.trim()
```

!!! warning

    Do not retain a previously returned `EntryFilterList` as request state. Encode query parameters with `HttpUrl.Builder` rather than concatenating user text into a URL.

## Errors and changing providers

- Let network and parsing failures reach Katari with useful context; do not convert them into an empty successful page.
- Retry only operations known to be safe and transient. Avoid retry loops inside source methods.
- Expect optional nodes and fields to disappear, but make required identity failures visible.
- Keep parsing functions small enough to test with saved, sanitized HTML or JSON fixtures.
- Preserve entry and chapter URLs when adapting to a redesigned provider site. See [publishing and maintenance](./publishing.md#preserve-content-identity).
