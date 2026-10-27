package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.chip.SuggestionChip
import mihon.presentation.kit.theme.KatariTheme

/**
 * One-tap languages that let a waiting translation continue, followed by [moreLabel] for the full picker.
 */
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
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        languages.forEach { language ->
            SuggestionChip(label = label(language), onClick = { onSelect(language) })
        }
        SuggestionChip(label = moreLabel, onClick = onMore)
    }
}
