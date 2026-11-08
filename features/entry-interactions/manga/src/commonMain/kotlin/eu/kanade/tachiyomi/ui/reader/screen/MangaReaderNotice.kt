package eu.kanade.tachiyomi.ui.reader.screen

import dev.icerock.moko.resources.StringResource

/** A short message the reader screen shows: one of Katari's own strings, or text a service already wrote out. */
internal sealed interface MangaReaderNotice {
    data class Resource(val message: StringResource) : MangaReaderNotice

    data class Text(val message: String) : MangaReaderNotice
}
