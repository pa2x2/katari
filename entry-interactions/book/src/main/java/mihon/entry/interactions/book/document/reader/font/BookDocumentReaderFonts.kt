package mihon.entry.interactions.book.document.reader.font

import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.viewer.settings.ViewerSettingBinding

/**
 * The open book's profile's fonts: the imported ones to pick from, the face each choice draws with, and the fall back
 * to Book when a chosen file is gone.
 */
internal class BookDocumentReaderFonts(
    private val store: BookDocumentReaderFontStore,
    private val profileId: Long,
) {
    private val importedFonts = MutableStateFlow(emptyList<BookDocumentReaderFont.Imported>())
    val imported: StateFlow<List<BookDocumentReaderFont.Imported>> = importedFonts.asStateFlow()

    // One family per file, so the typography built from it stays equal across recompositions and page measurements
    // made for one font are reused.
    private val families = mutableMapOf<String, FontFamily>()

    /** Null for [BookDocumentReaderFont.Book], and for an imported font whose file is gone. */
    fun family(font: BookDocumentReaderFont): FontFamily? = when (font) {
        BookDocumentReaderFont.Book -> null
        BookDocumentReaderFont.Serif -> FontFamily.Serif
        BookDocumentReaderFont.SansSerif -> FontFamily.SansSerif
        BookDocumentReaderFont.Monospace -> FontFamily.Monospace
        is BookDocumentReaderFont.Imported -> families[font.fileName]
            ?: store.file(profileId, font)?.let { FontFamily(Font(it)) }?.also { families[font.fileName] = it }
    }

    suspend fun refresh() {
        importedFonts.value = withContext(Dispatchers.IO) { store.importedFonts(profileId) }
    }

    /**
     * Resets the book's and the profile's choice where it names an imported font whose file is gone, such as after a
     * restore, which doesn't bring imported files back.
     */
    suspend fun forgetMissing(binding: ViewerSettingBinding<BookDocumentReaderFont>) {
        val setting = binding.state.value
        if (isMissing(setting.entryOverride)) binding.clearEntryOverride()
        if (isMissing(setting.profileValue)) binding.resetProfileValue()
    }

    /** Null when the file isn't a font. */
    suspend fun import(uri: Uri): BookDocumentReaderFont.Imported? {
        val font = withContext(Dispatchers.IO) { store.import(profileId, uri) }
        refresh()
        return font
    }

    suspend fun delete(font: BookDocumentReaderFont.Imported, binding: ViewerSettingBinding<BookDocumentReaderFont>) {
        withContext(Dispatchers.IO) { store.delete(profileId, font) }
        families.remove(font.fileName)
        refresh()
        forgetMissing(binding)
    }

    private suspend fun isMissing(font: BookDocumentReaderFont?): Boolean =
        font is BookDocumentReaderFont.Imported && withContext(Dispatchers.IO) { store.file(profileId, font) } == null
}
