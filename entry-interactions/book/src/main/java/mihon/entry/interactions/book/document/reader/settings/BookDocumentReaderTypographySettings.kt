package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.BookDocumentLineSpacing
import mihon.entry.interactions.reader.settings.BookDocumentMargins
import mihon.entry.interactions.reader.settings.BookDocumentTextAlignment
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderTypographySettings(bindings: BookDocumentReaderSettingBindings) {
    TypographyChoices(
        title = stringResource(MR.strings.pref_book_document_reader_line_spacing),
        binding = bindings.lineSpacing,
        choices = BookDocumentLineSpacing.entries,
        label = BookDocumentLineSpacing::stringRes,
    )
    TypographyChoices(
        title = stringResource(MR.strings.pref_book_document_reader_margins),
        binding = bindings.margins,
        choices = BookDocumentMargins.entries,
        label = BookDocumentMargins::stringRes,
    )
    TypographyChoices(
        title = stringResource(MR.strings.pref_book_document_reader_text_alignment),
        binding = bindings.textAlignment,
        choices = BookDocumentTextAlignment.entries,
        label = BookDocumentTextAlignment::stringRes,
    )
}

/** Picking the profile's own choice clears this book's override instead of pinning the book to it. */
@Composable
private fun <T> TypographyChoices(
    title: String,
    binding: ViewerSettingBinding<T>,
    choices: List<T>,
    label: (T) -> StringResource,
) {
    val setting by binding.state.collectAsState()
    val scope = rememberCoroutineScope()
    val profileDefault = setting.profileValue ?: setting.processorDefault
    ChipsRow(title = title) {
        choices.forEach { choice ->
            FilterChip(
                label = stringResource(label(choice)),
                selected = setting.effectiveValue == choice,
                onClick = {
                    scope.launch {
                        if (choice == profileDefault) {
                            binding.clearEntryOverride()
                        } else {
                            binding.setEntryOverride(choice)
                        }
                    }
                },
            )
        }
    }
}
