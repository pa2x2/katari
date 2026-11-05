package mihon.entry.interactions.manga.reader

import coil3.ComponentRegistry
import eu.kanade.tachiyomi.data.coil.ReaderPageFetcher
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder

fun addMangaReaderImageComponents(builder: ComponentRegistry.Builder): ComponentRegistry.Builder {
    addPlatformPageDecoders(builder)
    builder.add(TachiyomiImageDecoder.Factory())
    builder.add(ReaderPageFetcher.Factory())
    return builder
}

/** Decoders for the platform's own viewers, ahead of the format decoder. */
internal expect fun addPlatformPageDecoders(builder: ComponentRegistry.Builder)
