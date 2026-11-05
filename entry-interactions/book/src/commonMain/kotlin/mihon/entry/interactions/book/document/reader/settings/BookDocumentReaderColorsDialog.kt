package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.presentation.kit.content.ReadingPageSample
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.ColorSwatches
import mihon.presentation.kit.navigation.Tab
import mihon.presentation.kit.navigation.TabRow
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Picks a page and a text colour, from swatches or as hex, over a sample of the page they make. [onDelete] is set when
 * editing saved colours.
 */
@Composable
internal fun BookDocumentReaderColorsDialog(
    initial: BookDocumentReaderTheme.Custom,
    editing: Boolean,
    onSave: (BookDocumentReaderTheme.Custom) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var background by remember { mutableIntStateOf(initial.background) }
    var text by remember { mutableIntStateOf(initial.text) }
    var editingText by remember { mutableStateOf(false) }
    val color = if (editingText) text else background
    // The field keeps what is typed until it is a whole colour; picking a swatch or tab rewrites it.
    var hex by remember(editingText) { mutableStateOf(color.toHex()) }

    fun pick(picked: Int) {
        if (editingText) text = picked else background = picked
        hex = picked.toHex()
    }

    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(
            if (editing) MR.strings.book_document_reader_edit_colors else MR.strings.book_document_reader_new_colors,
        ),
        confirm = DialogAction(stringResource(MR.strings.action_save), {
            onSave(BookDocumentReaderTheme.Custom(background, text))
        }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismiss),
        neutral = onDelete?.let { DialogAction(stringResource(MR.strings.action_delete), it, destructive = true) },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            ReadingPageSample(
                palette = bookDocumentReaderPalette(BookDocumentReaderTheme.Custom(background, text)),
                text = stringResource(MR.strings.book_document_reader_colors_sample),
            )
            TabRow(container = SurfaceContainer.High) {
                Tab(
                    title = stringResource(MR.strings.book_document_reader_colors_background),
                    selected = !editingText,
                    onClick = { editingText = false },
                )
                Tab(
                    title = stringResource(MR.strings.book_document_reader_colors_text),
                    selected = editingText,
                    onClick = { editingText = true },
                )
            }
            ColorSwatches(
                colors = if (editingText) TEXT_SWATCHES else BACKGROUND_SWATCHES,
                selected = color,
                onSelect = ::pick,
            )
            TextField(
                value = hex,
                onValueChange = { typed ->
                    hex = typed.removePrefix("#").take(HEX_DIGITS).uppercase()
                    hex.toColorOrNull()?.let { if (editingText) text = it else background = it }
                },
                label = stringResource(MR.strings.book_document_reader_colors_hex),
                prefix = "#",
                isError = hex.toColorOrNull() == null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        }
    }
}

private fun Int.toHex(): String = (this and 0xFFFFFF).toString(16).padStart(HEX_DIGITS, '0').uppercase()

private fun String.toColorOrNull(): Int? =
    takeIf { it.length == HEX_DIGITS }?.toIntOrNull(16)?.let { it or OPAQUE }

private const val HEX_DIGITS = 6
private const val OPAQUE = 0xFF000000.toInt()

// Pages from white through paper and sepia tones to greys, night blue and black.
private val BACKGROUND_SWATCHES = listOf(
    0xFFFFFFFF,
    0xFFF8F4EA,
    0xFFF5EEDC,
    0xFFEAD9B8,
    0xFFE3EDE0,
    0xFFDDE4EC,
    0xFF3A3A3A,
    0xFF1E1B18,
    0xFF15202B,
    0xFF000000,
).map(Long::toInt)

// Text from black and warm browns to greys, cream and white.
private val TEXT_SWATCHES = listOf(
    0xFF000000,
    0xFF2B2721,
    0xFF4A3B2A,
    0xFF33415C,
    0xFF5F5F5F,
    0xFFB0B0B0,
    0xFFE3DACA,
    0xFFE6E6E6,
    0xFFF3E7C9,
    0xFFFFFFFF,
).map(Long::toInt)
