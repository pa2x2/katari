package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow
import mihon.book.api.document.BookDocumentFontFamily
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.viewer.settings.ResolvedViewerSetting
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
internal data class ProsePalette(val background: Color, val foreground: Color)

@Composable
internal fun prosePalette(theme: String, systemDark: Boolean): ProsePalette = when (theme) {
    HtmlProseSettingsProvider.THEME_LIGHT -> ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124))
    HtmlProseSettingsProvider.THEME_DARK -> ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5))
    HtmlProseSettingsProvider.THEME_SEPIA -> ProsePalette(Color(0xFFF4ECD8), Color(0xFF4B3A2A))
    HtmlProseSettingsProvider.THEME_BLACK -> ProsePalette(Color.Black, Color(0xFFE6E1E5))
    else -> if (systemDark) {
        ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5))
    } else {
        ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124))
    }
}

@Composable
internal fun rememberPaginatedProseTypefaces(
    chapters: Map<Long, HtmlProseLoadedChapter>,
): State<Map<Long, Map<String, Typeface>>> {
    val context = LocalContext.current
    return produceState(
        initialValue = emptyMap(),
        chapters,
    ) {
        val loadedByChapter = mutableMapOf<Long, Map<String, Typeface>>()
        chapters.forEach { (chapterId, chapter) ->
            val loader = chapter.resourceLoader ?: return@forEach
            val resourceIds = chapter.document.document.blocks.flatMapTo(linkedSetOf()) { block ->
                buildList {
                    (block.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let(::add)
                    block.inlineStyles.mapNotNullTo(this) { inline ->
                        (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
                    }
                }
            }
            val typefaces = resourceIds.mapNotNull { resourceId ->
                loader.loadProseTypeface(context, resourceId).getOrNull()?.let { resourceId to it }
            }.toMap()
            if (typefaces.isNotEmpty()) {
                loadedByChapter[chapterId] = typefaces
                value = loadedByChapter.toMap()
            }
        }
    }
}

internal fun proseTypeface(fontFamily: String): Typeface = when (fontFamily) {
    HtmlProseSettingsProvider.FONT_SANS_SERIF -> Typeface.SANS_SERIF
    HtmlProseSettingsProvider.FONT_MONOSPACE -> Typeface.MONOSPACE
    else -> Typeface.SERIF
}

internal fun String.toLayoutAlignment(): Layout.Alignment = when (this) {
    HtmlProseSettingsProvider.ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    HtmlProseSettingsProvider.ALIGN_LEFT -> Layout.Alignment.ALIGN_NORMAL
    else -> Layout.Alignment.ALIGN_NORMAL
}

internal fun String.toTextViewAlignment(): Int = when (this) {
    HtmlProseSettingsProvider.ALIGN_LEFT -> TextView.TEXT_ALIGNMENT_TEXT_START
    HtmlProseSettingsProvider.ALIGN_RIGHT -> TextView.TEXT_ALIGNMENT_TEXT_END
    else -> TextView.TEXT_ALIGNMENT_VIEW_START
}

internal fun Color.toReaderArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

internal fun EntryChapter.toTransitionItem() = ReaderEntryChildTransitionItem(name, scanlator)

internal fun transitionKey(transition: EntryChildTransition<EntryChapter>): String =
    "${transition.direction}:${transition.from.id}:${transition.to?.id ?: "terminal"}"

@Composable
internal fun <T> StateFlow<ResolvedViewerSetting<T>>.collectEffectiveValue(): State<T> {
    val resolved by collectAsState()
    return rememberUpdatedState(resolved.effectiveValue)
}
