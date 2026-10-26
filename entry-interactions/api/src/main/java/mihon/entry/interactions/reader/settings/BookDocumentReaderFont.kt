package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/** The face the book reader draws body text in, stored by [id]. [Book] keeps the faces the book asks for. */
sealed interface BookDocumentReaderFont {
    val id: String

    data object Book : BookDocumentReaderFont {
        override val id = "book"
    }

    data object Serif : BookDocumentReaderFont {
        override val id = "serif"
    }

    data object SansSerif : BookDocumentReaderFont {
        override val id = "sans-serif"
    }

    data object Monospace : BookDocumentReaderFont {
        override val id = "monospace"
    }

    /**
     * A TTF or OTF file imported into a profile, named by its file in the profile's font folder. Imported files are
     * not backed up, so a restored profile can point at one that isn't there.
     */
    data class Imported(val fileName: String) : BookDocumentReaderFont {
        override val id = "$IMPORTED_PREFIX$fileName"

        val displayName: String get() = fileName.substringBeforeLast('.')
    }

    companion object {
        private const val IMPORTED_PREFIX = "imported:"

        val system: List<BookDocumentReaderFont> = listOf(Book, Serif, SansSerif, Monospace)

        fun fromId(id: String): BookDocumentReaderFont? = system.firstOrNull { it.id == id }
            ?: id.removePrefix(IMPORTED_PREFIX).takeIf { id.startsWith(IMPORTED_PREFIX) && it.isNotBlank() }
                ?.let(::Imported)
    }
}

/** The name of a system face; null for an imported font, which goes by its [BookDocumentReaderFont.Imported.displayName]. */
val BookDocumentReaderFont.systemNameRes: StringResource?
    get() = when (this) {
        BookDocumentReaderFont.Book -> MR.strings.book_document_reader_typography_book
        BookDocumentReaderFont.Serif -> MR.strings.book_document_reader_font_serif
        BookDocumentReaderFont.SansSerif -> MR.strings.book_document_reader_font_sans_serif
        BookDocumentReaderFont.Monospace -> MR.strings.book_document_reader_font_monospace
        is BookDocumentReaderFont.Imported -> null
    }

/** The fonts a profile has imported for the book reader. */
interface BookDocumentReaderFontLibrary {
    fun importedFonts(profileId: Long): List<BookDocumentReaderFont.Imported>
}
