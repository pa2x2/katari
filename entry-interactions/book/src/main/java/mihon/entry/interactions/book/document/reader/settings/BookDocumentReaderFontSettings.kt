package mihon.entry.interactions.book.document.reader.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFonts
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.systemNameRes
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.list.FontChoiceRow
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The Font row, which opens the list of faces drawn in themselves, with the profile's imported fonts and an import. */
@Composable
internal fun BookDocumentReaderFontSettings(
    binding: ViewerSettingBinding<BookDocumentReaderFont>,
    fonts: BookDocumentReaderFonts,
) {
    val setting by binding.state.collectAsState()
    var picking by remember { mutableStateOf(false) }
    SettingsRow(
        title = stringResource(MR.strings.pref_book_document_reader_font),
        subtitle = setting.effectiveValue.name(),
        onClick = { picking = true },
    )
    if (picking) {
        BookDocumentReaderFontDialog(binding = binding, fonts = fonts, onDismiss = { picking = false })
    }
}

@Composable
private fun BookDocumentReaderFontDialog(
    binding: ViewerSettingBinding<BookDocumentReaderFont>,
    fonts: BookDocumentReaderFonts,
    onDismiss: () -> Unit,
) {
    val setting by binding.state.collectAsState()
    val imported by fonts.imported.collectAsState()
    val scope = rememberCoroutineScope()
    val messages = rememberScreenMessages()
    val importFailed = stringResource(MR.strings.book_document_reader_font_import_failed)
    val profileDefault = setting.profileValue ?: setting.processorDefault

    // Picking the profile's own font clears this book's override instead of pinning the book to it.
    fun choose(font: BookDocumentReaderFont) {
        scope.launch {
            if (font == profileDefault) binding.clearEntryOverride() else binding.setEntryOverride(font)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val font = fonts.import(uri)
            if (font == null) messages.show(importFailed) else choose(font)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.pref_book_document_reader_font),
        confirm = DialogAction(stringResource(MR.strings.action_close), onDismiss),
        neutral = DialogAction(
            label = stringResource(MR.strings.book_document_reader_font_import),
            onClick = { importLauncher.launch(FONT_MIME_TYPES) },
        ),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            (BookDocumentReaderFont.system + imported).forEach { font ->
                FontChoiceRow(
                    title = font.name(),
                    font = fonts.family(font),
                    selected = setting.effectiveValue == font,
                    onClick = { choose(font) },
                    trailing = (font as? BookDocumentReaderFont.Imported)?.let { importedFont ->
                        {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Delete,
                                contentDescription = stringResource(
                                    MR.strings.book_document_reader_font_delete,
                                    importedFont.displayName,
                                ),
                                onClick = { scope.launch { fonts.delete(importedFont, binding) } },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BookDocumentReaderFont.name(): String = when (this) {
    is BookDocumentReaderFont.Imported -> displayName
    else -> stringResource(requireNotNull(systemNameRes))
}

// Pickers label font files inconsistently, and some only as binary data.
private val FONT_MIME_TYPES = arrayOf(
    "font/ttf",
    "font/otf",
    "font/sfnt",
    "application/x-font-ttf",
    "application/x-font-otf",
    "application/vnd.ms-opentype",
    "application/octet-stream",
)
