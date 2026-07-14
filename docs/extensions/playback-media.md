# Playback media cookbook

Use `EntryHttpSource` for entries whose openable units resolve to video. Return `EntryMedia.Playback` with a descriptor containing the streams Katari can pass to its player.

## Return streams

```kotlin
override suspend fun getMedia(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): EntryMedia {
    val resolved = resolveEpisode(chapter, selection)
    return EntryMedia.Playback(
        PlaybackDescriptor(
            selection = resolved.selection,
            streams = resolved.streams.map { stream ->
                VideoStream(
                    request = VideoRequest(
                        url = stream.url,
                        headers = mapOf("Referer" to "$baseUrl/"),
                    ),
                    label = stream.label,
                    key = stream.key,
                    type = VideoStreamType.HLS,
                    mimeType = "application/x-mpegURL",
                )
            },
        ),
    )
}
```

Use `HLS` for HLS manifests, `DASH` for DASH manifests, and `PROGRESSIVE` for directly playable files. Use `UNKNOWN` only when the URL or response must determine the type. Set `mimeType` when the provider supplies reliable information.

!!! warning

    Put playback headers in `VideoRequest`; player requests do not automatically inherit the source's OkHttp headers. Include only what the media host requires, commonly `Referer`, `Origin`, or authorization. Never use a stream label or key to carry secrets.

## Dubs and source qualities

Expose selectable dimensions as stable keyed options:

```kotlin
val dubs = listOf(
    VideoPlaybackOption(key = "sub", label = "Subbed"),
    VideoPlaybackOption(key = "en", label = "English dub"),
)
val qualities = listOf(
    VideoPlaybackOption(key = "primary", label = "Primary server"),
    VideoPlaybackOption(key = "backup", label = "Backup server"),
)
```

Read `selection.dubKey`, `selection.sourceQualityKey`, and `selection.streamKey` when resolving media. The descriptor must report the selection actually returned:

```kotlin
val actualDub = selection.dubKey.takeIf(availableDubKeys::contains) ?: "sub"
val actualSelection = selection.copy(dubKey = actualDub)
```

If the requested option disappeared, fall back deterministically and put that fallback in `PlaybackDescriptor.selection`. Keep keys stable across releases even when user-facing labels change. Give every selectable stream a stable `VideoStream.key`.

## External subtitles

Implement `SubtitleSource` when subtitle tracks are resolved separately:

```kotlin
override suspend fun getSubtitles(
    chapter: SEntryChapter,
    selection: PlaybackSelection,
): List<VideoSubtitle> = resolveTracks(chapter, selection).map { track ->
    VideoSubtitle(
        request = VideoRequest(track.url, headers = track.headers),
        label = track.label,
        language = track.language,
        mimeType = track.mimeType,
        key = track.id,
        isDefault = track.isDefault,
        isForced = track.isForced,
    )
}
```

Language should be a recognized language tag when known. Mark defaults and forced tracks from provider metadata rather than guessing from the label.

## Expiring playback URLs

Resolve expiring URLs inside `getMedia()` instead of storing them in entry or chapter URLs. Keep the chapter URL as stable provider identity and obtain a fresh manifest or token when playback begins.

- Return all streams that are currently usable, not dead placeholders.
- Avoid probing every stream by downloading media during resolution.
- Preserve signed query strings and redirects.
- Give resolution failures useful context without including tokens in messages.
- Test unavailable requested options, empty stream lists, expiring URLs, required headers, and subtitle-only changes.

See [HTTP and parsing](./http-and-parsing.md) for safe request execution.
