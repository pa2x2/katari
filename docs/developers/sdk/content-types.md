# Content types

Content type belongs to each `SEntry`, not to an extension, factory, or source. A source can return entries of different supported types in the same catalogue when it can satisfy the contract for each entry.

## Current types

| `EntryType` | Child-item meaning                                 | Media contract          | Katari renderer |
| ----------- | -------------------------------------------------- | ----------------------- | --------------- |
| `MANGA`     | Chapter, volume, gallery, or another readable unit | `EntryMedia.ImagePages` | Reader          |
| `ANIME`     | Episode, movie, special, or another playable unit  | `EntryMedia.Playback`   | Video player    |

Set the type in catalogue results whenever the provider already exposes enough information:

```kotlin
SEntry.create().apply {
    url = item.path
    title = item.title
    type = if (item.isVideo) EntryType.ANIME else EntryType.MANGA
}
```

Do not defer the type until `getContentDetails()` unless the listing genuinely cannot determine it. Katari uses type information when presenting and opening catalogue results.

!!! note

    A source can additionally implement `SourceMetadata` to advertise all entry types it may supply. Katari can then show those types on source discovery surfaces before a catalogue is loaded. This metadata is optional and descriptive; it never replaces the type on each `SEntry`.

## Manga entries

For `MANGA`, child items are readable units and `getMedia()` returns ordered image descriptors. Implement `EntryImageSource`, normally by extending `EntryImageHttpSource`, so Katari can resolve and download image bytes on demand.

Return provider reading order. Reader direction and layout are user preferences managed by Katari rather than the source.

## Anime entries

For `ANIME`, child items are playable units and `getMedia()` returns a `PlaybackDescriptor`. The descriptor may expose multiple streams, dubs, source-quality options, and a resolved fallback selection. Implement `SubtitleSource` when external subtitle tracks are resolved separately.

Stream URLs may expire; keep stable episode identity in the child URL and resolve temporary playback requests when `getMedia()` is called.

## Mixed catalogues

A mixed catalogue must preserve the same lifecycle and identity guarantees for every returned entry. Branch on the entry or resolved provider data rather than global mutable source state:

```kotlin
override suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): EntryMedia = when (mediaKind(chapter)) {
    MediaKind.IMAGES -> resolveImagePages(chapter)
    MediaKind.VIDEO -> resolvePlayback(chapter, selection)
}
```

The child item does not currently carry `EntryType` itself. Its association with the parent entry and its stable source identity must therefore remain unambiguous.

## Support for additional content types

An extension can only use `EntryType` values supplied by the SDK and supported by the Katari runtime. It cannot introduce another value independently.

When another type becomes available, its SDK release identifies the child-item meaning, media contract, applicable capabilities, and first Katari version that supports it. Consult the [SDK changelog](./changelog.md) before using it; older Katari releases may reject an extension that requires the newer API family.
