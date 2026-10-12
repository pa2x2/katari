package mihon.entry.interactions.manga.reader

import coil3.ComponentRegistry
import eu.kanade.tachiyomi.data.coil.ReaderPageFetcher
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.data.coil.WebtoonStripsDecoder

fun addMangaReaderImageComponents(builder: ComponentRegistry.Builder): ComponentRegistry.Builder {
    // Ahead of the format decoder, which would otherwise take the long strip pages in the formats it handles
    builder.add(WebtoonStripsDecoder.Factory())
    builder.add(TachiyomiImageDecoder.Factory())
    builder.add(ReaderPageFetcher.Factory())
    return builder
}
