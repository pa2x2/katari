package mihon.entry.interactions.manga.reader

import coil3.ComponentRegistry
import eu.kanade.tachiyomi.data.coil.WebtoonStripsDecoder

// The format decoder would otherwise take the long strip pages in the formats it handles
internal actual fun addPlatformPageDecoders(builder: ComponentRegistry.Builder) {
    builder.add(WebtoonStripsDecoder.Factory())
}
