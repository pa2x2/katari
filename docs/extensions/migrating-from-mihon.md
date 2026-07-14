# Migrate a Mihon source

Katari continues to load selected legacy Mihon extension API families for compatibility. That support is not the authoring API for new Katari extensions. A migrated extension should depend only on `entry-source-api` and use Entry-native models from end to end.

## Contract mapping

| Mihon `source-api`      | Katari `entry-source-api`                       |
| ----------------------- | ----------------------------------------------- |
| `Source`                | `UnifiedSource`                                 |
| `CatalogueSource`       | `EntryCatalogueSource`                          |
| `HttpSource`            | `EntryHttpSource` or `EntryImageHttpSource`     |
| `SourceFactory`         | `EntrySourceFactory`                            |
| `SManga`                | `SEntry`                                        |
| `SChapter`              | `SEntryChapter`                                 |
| `MangasPage`            | `EntryPageResult<SEntry>`                       |
| `FilterList` / `Filter` | `EntryFilterList` / `EntryFilter`               |
| `Page`                  | `EntryImagePage` inside `EntryMedia.ImagePages` |
| RxJava return values    | Kotlin suspending functions                     |

Do not keep `source-api` as a second dependency merely to reuse its models. Convert the implementation fully or isolate temporary conversion outside the published Entry extension.

## Recommended migration order

1. Replace the Gradle dependency with `entry-source-api` as shown in [getting started](./getting-started.md).
2. Replace `SourceFactory` with `EntrySourceFactory`.
3. Move catalogue methods to suspending `UnifiedSource` methods.
4. Convert listing and detail parsers from `SManga` to `SEntry`.
5. Set `SEntry.type` for every returned item.
6. Convert chapter or episode models to `SEntryChapter`.
7. Replace page or video APIs with `getMedia()` returning `EntryMedia`.
8. Replace filters and preferences with their Entry equivalents.
9. Remove all imports from `eu.kanade.tachiyomi.source.*` except the `source.entry` package.

## Replace RxJava at the boundary

Legacy sources commonly returned `Observable` or `Single`. Entry sources expose `suspend` functions instead:

```kotlin
override suspend fun getContentDetails(entry: SEntry): SEntry {
    return client.newCall(detailsRequest(entry)).awaitSuccess().use { response ->
        parseDetails(response, entry)
    }
}
```

Keep cancellation cooperative by using the suspending network helpers supplied by the SDK rather than blocking a coroutine thread.

## Make type and media explicit

Mihon's models imply that every catalogue item is manga-like. Katari separates entry metadata from the media returned for a chapter.

For image content:

```kotlin
entry.type = EntryType.MANGA
return EntryMedia.ImagePages(pages)
```

For playback content:

```kotlin
entry.type = EntryType.ANIME
return EntryMedia.Playback(descriptor)
```

Do not select behavior from the extension package, factory, or catalogue name. Set the type on the entry and return the media that the chapter actually resolves.

## Preserve identity

Migration should not silently create duplicate sources or library entries. Preserve the existing source ID and stable entry and chapter URLs whenever the new implementation represents the same provider data.

If the old source inherited an ID generated from `HttpSource`, check the resulting ID before changing its name, language, or version inputs. An explicit `id` is safer when the public name must change.

## Factory example

A single migrated factory may return several source implementations, including sources that expose different content types:

```kotlin
class ExampleFactory : EntrySourceFactory {
    override fun createSources(): List<UnifiedSource> = listOf(
        ExampleComicsSource(),
        ExampleVideoSource(),
    )
}
```

!!! note

    There is no required extension-level content-type declaration. Katari derives behavior from entries and source capabilities. Repositories may optionally publish per-source `supportedEntryTypes` discovery metadata; legacy Mihon API families are inferred as Manga by the compatibility layer.
