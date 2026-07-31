package mihon.entry.interactions.book.epub

import android.content.Context
import android.content.Intent
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.BookReaderProcessor
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.StandardReaderCapabilities

/** Built-in Readium renderer for prepared EPUB publications. */
internal class ReadiumEpubReaderProcessor : BookReaderProcessor {
    override val id: String = "builtin.readium.epub"
    override val displayName: String = "EPUB reader"
    override val viewerSettingsSurfaceId = ReadiumEpubSettingsProvider.PROVIDER_ID
    override val potentialReaderCapabilities = TEXT_SELECTION_CAPABILITIES

    override fun supports(model: BookPublicationModelDescriptor): Boolean =
        model == ReadiumEpubPublicationModel.DESCRIPTOR

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = ReadiumEpubReaderActivity.newIntent(context, request, id, sessionToken)

    override fun readerCapabilities(model: BookPublicationModel) =
        if ((model as? ReadiumEpubPublicationModel)?.isFixedLayout == false) {
            TEXT_SELECTION_CAPABILITIES
        } else {
            emptySet()
        }

    private companion object {
        val TEXT_SELECTION_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        )
    }
}
