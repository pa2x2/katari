package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag

/**
 * One-tap languages that let a waiting translation continue, followed by [moreLabel] for the full picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TranslationLanguageSuggestionChips(
    languages: List<LanguageTag>,
    label: (LanguageTag) -> String,
    moreLabel: String,
    onSelect: (LanguageTag) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        languages.forEach { language ->
            SuggestionChip(
                onClick = { onSelect(language) },
                label = { Text(label(language)) },
            )
        }
        SuggestionChip(
            onClick = onMore,
            label = { Text(moreLabel) },
        )
    }
}
