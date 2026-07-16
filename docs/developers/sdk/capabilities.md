# Source capabilities

Capabilities are focused interfaces or properties that opt a source into behavior beyond the required `UnifiedSource` lifecycle. Katari should detect capabilities through their public contracts rather than through concrete source classes.

## Capability matrix

| Contract | Intent |
| --- | --- |
| `EntryCatalogueSource` | Makes a source browsable and declares language, latest-update support, and immersive-browsing support. |
| `SourceMetadata` | Advertises optional descriptive information, currently the entry types a source may supply. |
| `RelatedEntriesSource` | Supplies source-defined entries related to a selected entry. |
| `EntryImageSource` | Resolves and downloads ordered image pages. |
| `SubtitleSource` | Resolves external subtitle tracks for a playback selection. |
| `EntryPreviewSource` | Supplies ordered static preview images describing an entry independently of child media. |
| `ConfigurableSource` | Adds an Android preference screen and source-scoped preferences. |
| `ResolvableSource` | Classifies and resolves supported external entry or child URLs. |
| `SourceHomePage` | Supplies a browser home page for the source. |
| `WebViewSource` | Supplies a canonical entry details URL and WebView headers. |
| `ChapterWebViewSource` | Adds a canonical URL for an openable child item. |
| `EntryItemOrientationProvider` | Selects vertical or horizontal catalogue thumbnails. |
| `EmptyChapterListSource` | Declares that a successful child-list response may legitimately be empty. |
| `IncrementalChapterSource` | Receives currently stored child items while refreshing the list. |
| `ChapterNumberRecognitionSource` | Requests host-side number recognition for unknown child numbers. |
| `UnmeteredSource` | Excludes the source from Katari's metered-source update warning. |

## Catalogue presentation

`EntryCatalogueSource.supportsLatest` controls whether Katari offers the latest-updates catalogue call. Return `true` only when `getLatestUpdates()` has a meaningful implementation.

`supportsImmersiveFeed` opts the source into Katari's immersive catalogue and feed presentation. It does not change the catalogue response shape; entries must still carry supported types and satisfy their normal media contracts.

Only opt in when the entry types shown by those catalogue or feed surfaces have an immersive runtime. BOOK entries do not currently have one, so a catalogue that can return BOOK should leave this disabled unless unsupported entries are excluded from its immersive listings.

`EntryItemOrientationProvider.itemOrientation` controls source thumbnails in browse, library, and feeds. It is presentation metadata, not content-type classification.

## Source metadata

Implement `SourceMetadata` when Katari can describe the source's catalogue before loading it:

```kotlin
class ExampleSource : EntryHttpSource(), SourceMetadata {
    override val supportedEntryTypes = setOf(EntryType.MANGA, EntryType.ANIME, EntryType.BOOK)
}
```

Include every entry type the source may return. Katari presents this information as a subtle source-level hint; it does not validate or restrict catalogue results. Each returned `SEntry.type` remains authoritative. Omitting the capability, or returning an empty set, means that the source's supported types are unknown.

## Related entries

Implement `RelatedEntriesSource` when the provider exposes entries related to a selected entry:

```kotlin
class ExampleSource : EntryHttpSource(), RelatedEntriesSource {
    override suspend fun getRelatedEntries(entry: SEntry): List<SEntry> {
        return fetchRelatedEntries(entry.url)
    }
}
```

The capability itself is the support signal; there is no separate `supportsRelatedEntries` flag.

Return entries from the same source in the provider's display order, with stable URLs suitable for the normal entry-details flow. Each returned `SEntry.type` is authoritative, and a result may contain mixed entry types. An empty list means that the source has no related entries for the selected entry.

Related entries are provider-defined discovery results. Katari does not synthesize them through title search when the capability is absent or a request returns no results.

## Child-list safety

Katari normally treats an unexpectedly empty refreshed child list as a possible transient parser failure. Implement `EmptyChapterListSource` only when an empty list is valid provider data.

`IncrementalChapterSource` is for sources whose refresh algorithm needs the currently stored list:

```kotlin
class ExampleSource : EntryHttpSource(), IncrementalChapterSource {
    override suspend fun getChapterList(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
    ): List<SEntryChapter> {
        return fetchChangesSince(existingChapters.maxOfOrNull { it.dateUpload })
    }
}
```

The returned list must still represent the source's authoritative refreshed state according to the capability contract. Do not mutate the supplied list or assume that refreshes are serialized.

Use `ChapterNumberRecognitionSource` when the provider does not supply reliable numbers and Katari should infer unknown values from child names. Explicit source numbers remain authoritative.

## Images and previews

`EntryImageSource` is the byte-loading contract for `EntryMedia.ImagePages`. It separates page discovery, optional lazy URL resolution, request construction, and image downloading. Use `EntryImageHttpSource` when Katari's shared client and default request behavior are appropriate.

`EntryPreviewSource` supplies entry-level preview images rather than reader pages. Preview indices define display order; URLs may include optional titles or canonical links. Whether a type consumes previews is determined by the Katari runtime, so an extension should provide a reasonable baseline experience when previews are unavailable.

## Playback and subtitles

`SubtitleSource.getSubtitles()` receives the same child and playback selection used for media resolution. Return stable keys where possible, accurate language tags when known, and request headers required by the subtitle host.

The capability is separate from `PlaybackDescriptor.streams`; an empty subtitle list is a valid result.

## Book resources and processors

A book source describes resources with `BookResourceCatalog`, `BookSourceResource`, and closed `BookResourceLocation` values. Katari converts that source view into the processor-facing contracts from `book-api`. Processors never receive the `UnifiedSource`, its HTTP client, or source-defined executable behavior.

This is intentionally not modeled as `EpubSource` or another format-specific capability. Format support belongs to independently selectable processors, each of which may supply a completely different reader. An unsupported format therefore produces a structured unavailable result rather than falling back to another media contract.

## URLs and WebView integration

`SourceHomePage`, `WebViewSource`, and `ChapterWebViewSource` expose canonical browser destinations. Return absolute URLs and include only headers required for the WebView request. Do not expose cookies, tokens, or other secrets through URLs or diagnostic messages.

`ResolvableSource` handles the opposite direction: given an external URI, classify it as an entry, child item, or unknown before resolving it. Return `Unknown` for unsupported URLs and `null` when a recognized URI can no longer be resolved.

## Preferences

`ConfigurableSource` receives an `EntryPreferenceScreen` and exposes `SharedPreferences` scoped by stable source ID. Preference keys become persisted extension state; do not rename or repurpose them without a migration.

Keep authentication values out of logs and public error messages. Removing a preference should not leave request construction in an invalid state.

## Metered-source warning

`UnmeteredSource` affects Katari's warning policy during library updates. It does not alter Android network accounting, force an unmetered transport, or guarantee that the provider transfers little data.

Only opt in when the source's update traffic should intentionally be excluded from that warning.
