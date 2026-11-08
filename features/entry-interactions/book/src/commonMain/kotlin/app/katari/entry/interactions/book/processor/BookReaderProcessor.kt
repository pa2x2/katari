package app.katari.entry.interactions.book.processor

import app.katari.book.api.model.BookPublicationModel
import app.katari.book.api.model.BookPublicationModelDescriptor
import app.katari.entry.viewer.settings.shared.ReaderCapabilityId
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingId

/**
 * A reader for an already prepared publication model: what it reads and what it offers. Starting it is the platform's
 * part, `createReaderIntent` on Android.
 */
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

    /** Shared reader settings this processor's settings surface turns on until the user changes them. */
    val sharedSettingsOnByDefault: Set<ReaderSharedSettingId>
        get() = emptySet()

    fun supports(model: BookPublicationModelDescriptor): Boolean

    /** Capabilities which are effective for this concrete prepared model. */
    fun readerCapabilities(model: BookPublicationModel): Set<ReaderCapabilityId> = emptySet()
}

internal data class BookReaderRequest(
    val entryId: Long,
    val chapterId: Long,
)
