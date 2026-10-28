package eu.kanade.tachiyomi.ui.entry.cover

import mihon.core.platform.PlatformContext
import tachiyomi.domain.storage.DocumentUri

/**
 * Writes out the image the cover viewer shows, which [data] is the image loader's model of: an entry's cover or a
 * source image. Where the platform has a shared pictures folder Save writes straight into it; elsewhere the user picks
 * the file to save to. Each call throws when the image can't be loaded or written, for the caller to report.
 */
interface EntryCoverExport {
    /** False where the platform has no share sheet; Share is then hidden. */
    val isSharingAvailable: Boolean

    /** True where Save writes into a document the user picks, through [saveToDocument], not [saveToPictures]. */
    val savesToPickedDocument: Boolean

    /** Saves the image as [name] into the platform's pictures folder. Only called without [savesToPickedDocument]. */
    suspend fun saveToPictures(context: PlatformContext, data: Any, name: String)

    /** Replaces [document]'s content with the image. Only called with [savesToPickedDocument]. */
    suspend fun saveToDocument(context: PlatformContext, data: Any, document: DocumentUri)

    /** Hands the image, named [name], to an app the user picks. Only called while [isSharingAvailable]. */
    suspend fun share(context: PlatformContext, data: Any, name: String)
}
