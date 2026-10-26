package mihon.presentation.kit.theme

import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The colours of a reading page, for content renderers such as the book reader: the page [background], its text
 * [foreground], an [accent] for links and progress, and a [warning] colour. The reader's user picks one of the presets
 * below; [ReadingPaletteTheme] derives every kit colour role from it so chrome, sheets and dialogs over the page match
 * it. Colours drawn on the page itself come from the properties here rather than from KatariTheme.colors.
 */
@Immutable
data class ReadingPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val warning: Color,
) {
    /** Rules, quotation bars and other lines drawn on the page. */
    val outline: Color get() = foreground.copy(alpha = OUTLINE_ALPHA)

    /** Faint fill behind tables and under progress tracks. */
    val fill: Color get() = foreground.copy(alpha = FILL_ALPHA)

    /** Text that stands in for missing content, such as an image's description when the image cannot load. */
    val foregroundVariant: Color get() = foreground.copy(alpha = FOREGROUND_VARIANT_ALPHA)

    /** Reading progress drawn over the page. */
    val progress: Color get() = accent.copy(alpha = PROGRESS_ALPHA)

    /** Highlight behind selected text. */
    val selection: Color get() = accent.copy(alpha = SELECTION_ALPHA)

    /** Highlight behind each place a search found; [currentSearchMatch] marks the one being shown. */
    val searchMatch: Color get() = accent.copy(alpha = SEARCH_MATCH_ALPHA)

    val currentSearchMatch: Color get() = accent.copy(alpha = CURRENT_SEARCH_MATCH_ALPHA)

    companion object {
        /**
         * A page in colours the user picked, as opaque ARGB [background] and [foreground]. Links, selection and
         * progress follow the text colour, and warnings take the preset warning colour that reads on the page.
         */
        fun custom(@ColorInt background: Int, @ColorInt foreground: Int): ReadingPalette {
            val page = Color(background)
            return ReadingPalette(
                background = page,
                foreground = Color(foreground),
                accent = Color(foreground),
                warning = if (page.luminance() < DARK_PAGE_LUMINANCE) Dusk.warning else Paper.warning,
            )
        }

        /** The app theme's own colours. */
        val App: ReadingPalette
            @Composable
            @ReadOnlyComposable
            get() {
                val colors = KatariTheme.colors
                return ReadingPalette(colors.background, colors.onBackground, colors.primary, colors.error)
            }

        /** Dark brown text on warm paper. */
        val Paper: ReadingPalette = ReadingPalette(
            background = Color(0xFFF5EEDC),
            foreground = Color(0xFF2B2721),
            accent = Color(0xFF735C2E),
            warning = Color(0xFF9F3D37),
        )

        /** Warm light text on a dark brown page. */
        val Dusk: ReadingPalette = ReadingPalette(
            background = Color(0xFF1E1B18),
            foreground = Color(0xFFE3DACA),
            accent = Color(0xFFD5A85B),
            warning = Color(0xFFFFB4AB),
        )

        /** Light grey text on pure black for OLED screens; warnings keep the app's error colour. */
        val Black: ReadingPalette
            @Composable
            @ReadOnlyComposable
            get() = ReadingPalette(
                background = Color.Black,
                foreground = BLACK_PAGE_TEXT,
                accent = BLACK_PAGE_TEXT,
                warning = KatariTheme.colors.error,
            )
    }
}

private val BLACK_PAGE_TEXT = Color(0xFFE6E6E6)
private const val DARK_PAGE_LUMINANCE = 0.5f
private const val OUTLINE_ALPHA = 0.35f
private const val FILL_ALPHA = 0.08f
private const val FOREGROUND_VARIANT_ALPHA = 0.72f
private const val PROGRESS_ALPHA = 0.75f
private const val SELECTION_ALPHA = 0.4f
private const val SEARCH_MATCH_ALPHA = 0.2f
private const val CURRENT_SEARCH_MATCH_ALPHA = 0.55f
