package eu.kanade.tachiyomi.ui.entry.cover

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.DocumentUri
import java.io.InputStream

/** The image the user picked as an entry's new cover, and the cover file a local entry keeps beside its chapters. */
interface EntryCoverDocuments {
    /** False where local entries' folders can't be written, so their covers can't be edited. */
    val editsLocalCovers: Boolean

    /** Null when [document] can't be opened. */
    fun open(document: DocumentUri): InputStream?

    /** Replaces local entry [entry]'s cover with [image]. Only called while [editsLocalCovers]. */
    fun replaceLocalCover(entry: Entry, image: InputStream)
}
