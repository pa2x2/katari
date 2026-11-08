package app.katari.entry.interactions.book.document.reader.font

import androidx.compose.ui.text.font.FontFamily
import app.katari.entry.interactions.book.document.resource.BookDocumentResourceDecoding
import app.katari.entry.interactions.reader.settings.BookDocumentReaderFont
import app.katari.entry.viewer.settings.ViewerSettingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.storage.DocumentUri

/**
 * The open book's profile's fonts: the imported ones to pick from, the face each choice draws with, and the fall back
 * to Book when a chosen file isn't here. A choice naming such a file is kept, since the file may still come, as a font
 * imported on another device does once sync fetches it; only deleting the font here gives the choice up.
 */
internal class BookDocumentReaderFonts(
    private val store: BookDocumentReaderFontStore,
    private val documents: StorageFileResolver,
    private val decoding: BookDocumentResourceDecoding,
    private val profileId: Long,
) {
    private val importedFonts = MutableStateFlow(emptyList<BookDocumentReaderFont.Imported>())
    val imported: StateFlow<List<BookDocumentReaderFont.Imported>> = importedFonts.asStateFlow()

    // One family per file, so the typography built from it stays equal across recompositions and page measurements
    // made for one font are reused.
    private val families = mutableMapOf<String, FontFamily>()

    /** Null for [BookDocumentReaderFont.Book], and for an imported font whose file is gone or no longer reads. */
    fun family(font: BookDocumentReaderFont): FontFamily? = when (font) {
        BookDocumentReaderFont.Book -> null
        BookDocumentReaderFont.Serif -> FontFamily.Serif
        BookDocumentReaderFont.SansSerif -> FontFamily.SansSerif
        BookDocumentReaderFont.Monospace -> FontFamily.Monospace
        is BookDocumentReaderFont.Imported -> families[font.fileName]
            ?: store.file(profileId, font)
                ?.let { file -> runCatching { decoding.fontFamily(file.readBytes()) }.getOrNull() }
                ?.also { families[font.fileName] = it }
    }

    suspend fun refresh() {
        importedFonts.value = withContext(Dispatchers.IO) { store.importedFonts(profileId) }
    }

    /** Imports the document picked at [uri], kept only when it reads as a font; null when it doesn't. */
    suspend fun import(uri: DocumentUri): BookDocumentReaderFont.Imported? {
        val font = withContext(Dispatchers.IO) {
            val document = documents.fromUri(uri.value)
            store.import(
                profileId = profileId,
                open = { document?.openInputStream() },
                isFont = { file -> runCatching { decoding.fontFamily(file.readBytes()) }.isSuccess },
                displayName = { document?.name },
            )
        }
        refresh()
        return font
    }

    /** Deletes the font, and the book's and the profile's choice of it. */
    suspend fun delete(font: BookDocumentReaderFont.Imported, binding: ViewerSettingBinding<BookDocumentReaderFont>) {
        withContext(Dispatchers.IO) { store.delete(profileId, font) }
        families.remove(font.fileName)
        refresh()
        val setting = binding.state.value
        if (setting.entryOverride == font) binding.clearEntryOverride()
        if (setting.profileValue == font) binding.resetProfileValue()
    }
}
