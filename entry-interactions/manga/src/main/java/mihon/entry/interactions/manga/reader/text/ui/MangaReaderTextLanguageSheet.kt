package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.ui.picker.language.TranslationLanguageDefaultOption
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.translationLanguageOptionsOf
import mihon.translation.ui.picker.language.translationRecentLanguageOptions
import mihon.translation.ui.presentation.TranslationPickerSheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Chooses the language pages are read in, with the searchable language list the reader's translation pickers use.
 * A [required] choice asks which language the text of a multi-language source is in. When the source declares a
 * [sourceLanguage], the first row follows it instead of keeping a language for the series.
 */
@Composable
internal fun MangaReaderTextLanguageSheet(
    languages: List<LanguageTag>,
    required: Boolean,
    selected: LanguageTag?,
    recentLanguages: List<LanguageTag>,
    sourceLanguage: LanguageTag?,
    followsSource: Boolean,
    onChoose: (LanguageTag) -> Unit,
    onFollowSource: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(languages) { translationLanguageOptionsOf(languages) }
    val recents = remember(recentLanguages, options) { translationRecentLanguageOptions(recentLanguages, options) }
    TranslationPickerSheet(
        title = stringResource(
            if (required) {
                MR.strings.reader_text_language_required
            } else {
                MR.strings.reader_text_page_language
            },
        ),
        onDismiss = onDismiss,
    ) {
        TranslationLanguagePickerList(
            options = options,
            selected = selected,
            onSelect = onChoose,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(top = KatariTheme.spacing.s),
            defaultOption = sourceLanguage?.let { language ->
                TranslationLanguageDefaultOption(
                    label = stringResource(MR.strings.reader_text_page_language_follow_source),
                    supporting = language.displayName(),
                    selected = followsSource,
                )
            },
            onSelectDefault = onFollowSource,
            shortcuts = recents,
        )
    }
}
