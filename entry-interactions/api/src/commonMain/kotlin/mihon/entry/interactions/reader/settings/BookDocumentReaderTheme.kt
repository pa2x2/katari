package mihon.entry.interactions.reader.settings

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/**
 * The colours of the book reader's pages, stored by [id]: one of the presets, or colours the user picked. A custom
 * theme carries its colours itself, so a book keeps them when the profile's saved colours change.
 */
sealed interface BookDocumentReaderTheme {
    val id: String

    // Preset ids are the names of the enum they replaced, so stored choices still read.
    data object App : BookDocumentReaderTheme {
        override val id = "APP"
    }

    data object Paper : BookDocumentReaderTheme {
        override val id = "PAPER"
    }

    data object Dusk : BookDocumentReaderTheme {
        override val id = "DUSK"
    }

    data object Black : BookDocumentReaderTheme {
        override val id = "BLACK"
    }

    /** [background] and [text] are opaque ARGB colours. */
    data class Custom(val background: Int, val text: Int) : BookDocumentReaderTheme {
        override val id = "$CUSTOM_PREFIX${background.toHex()}:${text.toHex()}"
    }

    companion object {
        private const val CUSTOM_PREFIX = "CUSTOM:"

        val presets: List<BookDocumentReaderTheme> = listOf(App, Paper, Dusk, Black)

        fun fromId(id: String): BookDocumentReaderTheme? = presets.firstOrNull { it.id == id }
            ?: id.removePrefix(CUSTOM_PREFIX).takeIf { id.startsWith(CUSTOM_PREFIX) }
                ?.split(':')
                ?.takeIf { it.size == 2 }
                ?.let { (background, text) ->
                    val backgroundColor = background.toColorOrNull() ?: return null
                    val textColor = text.toColorOrNull() ?: return null
                    Custom(backgroundColor, textColor)
                }

        /** The profile's saved colours, stored as one string. */
        fun encodeCustomList(themes: List<Custom>): String = themes.joinToString(LIST_SEPARATOR) { it.id }

        fun decodeCustomList(value: String): List<Custom> =
            value.split(LIST_SEPARATOR).mapNotNull { fromId(it) as? Custom }.distinct()

        private const val LIST_SEPARATOR = ";"
    }
}

/** The name of a preset; null for custom colours, which have none. */
val BookDocumentReaderTheme.presetNameRes: StringResource?
    get() = when (this) {
        BookDocumentReaderTheme.App -> MR.strings.book_document_reader_theme_app
        BookDocumentReaderTheme.Paper -> MR.strings.book_document_reader_theme_paper
        BookDocumentReaderTheme.Dusk -> MR.strings.book_document_reader_theme_dusk
        BookDocumentReaderTheme.Black -> MR.strings.book_document_reader_theme_black
        is BookDocumentReaderTheme.Custom -> null
    }

private fun Int.toHex(): String = (this and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

private fun String.toColorOrNull(): Int? =
    takeIf { it.length == 6 }?.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }
