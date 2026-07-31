package mihon.entry.interactions.book.prose

import android.content.Context
import android.content.Intent
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.BookReaderProcessor
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.StandardReaderCapabilities

/** Built-in native renderer for prepared structured-prose publications. */
internal class NativeHtmlProseReaderProcessor : BookReaderProcessor {
    override val id: String = "builtin.html.prose-chapter"
    override val displayName: String = "Prose chapter reader"
    override val viewerSettingsSurfaceId = HtmlProseSettingsProvider.PROVIDER_ID
    override val potentialReaderCapabilities = PROSE_READER_CAPABILITIES

    override fun supports(model: BookPublicationModelDescriptor): Boolean =
        model == BookDocumentPublicationModel.DESCRIPTOR

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = HtmlProseChapterReaderActivity.newIntent(context, request, id, sessionToken)

    override fun readerCapabilities(model: BookPublicationModel) =
        if (model is BookDocumentPublicationModel) PROSE_READER_CAPABILITIES else emptySet()

    private companion object {
        val PROSE_READER_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
            StandardReaderCapabilities.NextChapterPreparation,
        )
    }
}
