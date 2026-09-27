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
import mihon.translation.ui.picker.language.TranslationLanguagePickerList
import mihon.translation.ui.picker.language.translationLanguageOption
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.Collator

/**
 * Asks which language the text of a multi-language source is in, with the searchable language list the reader's
 * translation pickers use.
 */
@Composable
internal fun MangaReaderTextLanguageSheet(
    languages: List<LanguageTag>,
    onChoose: (LanguageTag) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember(languages) {
        val collator = Collator.getInstance()
        languages.map { translationLanguageOption(it) }.sortedWith { first, second ->
            collator.compare(first.displayName, second.displayName)
        }
    }
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
                        text = stringResource(MR.strings.reader_text_language_required),
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
                    selected = null,
                    onSelect = onChoose,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(top = 8.dp),
                )
            }
        }
    }
}
