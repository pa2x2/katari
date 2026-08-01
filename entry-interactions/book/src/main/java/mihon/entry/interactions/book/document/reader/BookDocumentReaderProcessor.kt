package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.content.Intent
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.processor.BookReaderProcessor
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities

/** Built-in native reader for semantic BOOK documents. */
internal class BookDocumentReaderProcessor : BookReaderProcessor {
    override val id = PROCESSOR_ID
    override val displayName = "Document reader"
    override val viewerSettingsSurfaceId = SETTINGS_SURFACE_ID
    override val potentialReaderCapabilities = CAPABILITIES

    override fun supports(model: BookPublicationModelDescriptor): Boolean =
        model == BookDocumentPublicationModel.DESCRIPTOR

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = BookDocumentReaderActivity.newIntent(context, request, sessionToken)

    override fun readerCapabilities(model: BookPublicationModel) =
        CAPABILITIES.takeIf { model is BookDocumentPublicationModel }.orEmpty()

    companion object {
        const val PROCESSOR_ID = "builtin.book.document"
        const val SETTINGS_SURFACE_ID = "builtin.book.document"

        internal val CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
            StandardReaderCapabilities.NextChapterPreparation,
        )
    }
}
