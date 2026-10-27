package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.entry.interactions.reader.settings.presetNameRes
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Palette
import mihon.presentation.kit.content.ReadingPaletteSwatch
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * The presets, then the profile's saved colours, each a theme of its own; a long press edits or deletes saved colours.
 * Colours a book still uses after they were deleted from the list stay listed while the book shows them.
 */
@Composable
internal fun BookDocumentReaderThemeSettings(
    bindings: BookDocumentReaderSettingBindings,
) {
    val publisherStylesBinding = bindings.publisherStyles
    val publisherStyles by publisherStylesBinding.state.collectAsState()
    val publisherStylesDefault = publisherStyles.profileValue ?: publisherStyles.processorDefault
    val setting by bindings.theme.state.collectAsState()
    val saved by bindings.savedColors.collectAsState()
    val savedColors = remember(bindings) { BookDocumentReaderSavedColors(bindings.savedColors, bindings.theme) }
    val scope = rememberCoroutineScope()
    var colorsDialog by remember { mutableStateOf<ColorsDialog?>(null) }
    val unsaved = (setting.effectiveValue as? BookDocumentReaderTheme.Custom)?.takeUnless { it in saved }
    RowGroupHeader(stringResource(R.string.book_document_reader_theme))
    RowGroup {
        (BookDocumentReaderTheme.presets + saved + listOfNotNull(unsaved)).forEach { theme ->
            row(key = theme.id) {
                ChoiceRow(
                    title = theme.label(saved),
                    selected = setting.effectiveValue == theme,
                    onClick = { scope.launch { savedColors.choose(theme) } },
                    trailing = (theme as? BookDocumentReaderTheme.Custom)?.let {
                        { ReadingPaletteSwatch(bookDocumentReaderPalette(it)) }
                    },
                    onLongClick = (theme as? BookDocumentReaderTheme.Custom)?.takeIf { it in saved }?.let {
                        { colorsDialog = ColorsDialog(editing = it) }
                    },
                )
            }
        }
        row(key = NEW_COLORS_ROW) {
            SettingsRow(
                title = mokoStringResource(MR.strings.book_document_reader_new_colors),
                icon = MaterialSymbols.Rounded.Palette,
                onClick = { colorsDialog = ColorsDialog(editing = null) },
            )
        }
        row(key = PUBLISHER_STYLES_ROW) {
            ToggleRow(
                title = stringResource(R.string.book_document_reader_publisher_styles),
                subtitle = stringResource(R.string.book_document_reader_publisher_styles_summary),
                checked = publisherStyles.effectiveValue,
                onCheckedChange = { target ->
                    scope.launch {
                        if (target == publisherStylesDefault) {
                            publisherStylesBinding.clearEntryOverride()
                        } else {
                            publisherStylesBinding.setEntryOverride(target)
                        }
                    }
                },
            )
        }
    }
    colorsDialog?.let { dialog ->
        val dismiss = { colorsDialog = null }
        BookDocumentReaderColorsDialog(
            initial = dialog.editing ?: (setting.effectiveValue as? BookDocumentReaderTheme.Custom) ?: NEW_COLORS,
            editing = dialog.editing != null,
            onSave = { colors ->
                scope.launch { savedColors.save(colors, replacing = dialog.editing) }
                dismiss()
            },
            onDismiss = dismiss,
            onDelete = dialog.editing?.let { editing ->
                {
                    scope.launch { savedColors.delete(editing) }
                    dismiss()
                }
            },
        )
    }
}

/** [editing] is the saved colours being edited; null for new ones. */
private data class ColorsDialog(val editing: BookDocumentReaderTheme.Custom?)

@Composable
private fun BookDocumentReaderTheme.label(saved: List<BookDocumentReaderTheme.Custom>): String = when (this) {
    is BookDocumentReaderTheme.Custom -> when (val index = saved.indexOf(this)) {
        -1 -> mokoStringResource(MR.strings.book_document_reader_theme_custom_unsaved)
        else -> mokoStringResource(MR.strings.book_document_reader_theme_custom, index + 1)
    }
    else -> mokoStringResource(requireNotNull(presetNameRes))
}

private const val NEW_COLORS_ROW = "new-colors"
private const val PUBLISHER_STYLES_ROW = "publisher-styles"

// Dark brown on warm paper, a start the user then changes.
private val NEW_COLORS = BookDocumentReaderTheme.Custom(background = 0xFFF8F4EA.toInt(), text = 0xFF2B2721.toInt())
