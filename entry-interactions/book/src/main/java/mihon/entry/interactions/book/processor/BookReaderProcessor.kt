package mihon.entry.interactions.book.processor

import android.content.Context
import android.content.Intent
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.viewer.settings.shared.ReaderCapabilityId

/** Selects and launches a reader for an already prepared publication model. */
internal interface BookReaderProcessor {
    /** Stable across app updates so a remembered user choice can be restored. */
    val id: String

    /** User-facing processor name for the compatibility chooser. */
    val displayName: String

    /** App-level viewer settings surface configured by this processor, when one exists. */
    val viewerSettingsSurfaceId: String?
        get() = null

    /** Capabilities discoverable before a publication is opened. */
    val potentialReaderCapabilities: Set<ReaderCapabilityId>
        get() = emptySet()

    fun supports(model: BookPublicationModelDescriptor): Boolean

    /** Creates the reader-owned UI entry point for a prepared BOOK session. */
    fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent

    /** Capabilities which are effective for this concrete prepared model. */
    fun readerCapabilities(model: BookPublicationModel): Set<ReaderCapabilityId> = emptySet()
}

internal data class BookReaderRequest(
    val entryId: Long,
    val chapterId: Long,
)
