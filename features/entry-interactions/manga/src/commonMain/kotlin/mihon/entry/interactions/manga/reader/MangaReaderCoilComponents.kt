package mihon.entry.interactions.manga.reader

import coil3.ComponentRegistry
import eu.kanade.tachiyomi.data.coil.ReaderPageFetcher
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder

fun addMangaReaderImageComponents(builder: ComponentRegistry.Builder): ComponentRegistry.Builder {
    builder.add(TachiyomiImageDecoder.Factory())
    builder.add(ReaderPageFetcher.Factory())
    return builder
}
