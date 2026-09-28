package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.components.AdaptiveSheet
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.TranslationLanguageDefaultOption
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.picker.language.translationLanguageOptionsOf
import mihon.translation.ui.picker.language.translationRecentLanguageOptions
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
    BoxWithConstraints {
        AdaptiveSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.85f),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (required) {
                                MR.strings.reader_text_language_required
                            } else {
                                MR.strings.reader_text_page_language
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(MR.strings.action_close))
                    }
                }
                HorizontalDivider()
                TranslationLanguagePickerList(
                    options = options,
                    selected = selected,
                    onSelect = onChoose,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(top = 8.dp),
                    defaultOption = sourceLanguage?.let { language ->
                        TranslationLanguageDefaultOption(
                            label = stringResource(MR.strings.reader_text_page_language_follow_source),
                            supporting = language.displayName(),
                            selected = followsSource,
                        )
                    },
                    onSelectDefault = onFollowSource,
                    recents = recents,
                )
            }
        }
    }
}
