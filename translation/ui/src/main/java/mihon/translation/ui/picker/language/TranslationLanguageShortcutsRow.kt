package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.theme.KatariTheme

/** The [options] among [recentLanguages], most recent first, so recents never offer a language the list lacks. */
fun translationRecentLanguageOptions(
    recentLanguages: List<LanguageTag>,
    options: List<TranslationLanguageOption>,
): List<TranslationLanguageOption> = recentLanguages.mapNotNull { recent -> options.firstOrNull { it.tag == recent } }

/**
 * One-tap candidates under [title], such as the most recently used languages, rendered above the full picker list.
 *
 * Recents are pre-filtered by the caller with [translationRecentLanguageOptions].
 */
@Composable
fun TranslationLanguageShortcutsRow(
    title: String,
    shortcuts: List<TranslationLanguageOption>,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = KatariTheme.spacing.rowInset),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            items(shortcuts, key = { it.tag.value }) { option ->
                FilterChip(
                    label = option.nativeName,
                    selected = option.tag == selected,
                    onClick = { onSelect(option.tag) },
                )
            }
        }
    }
}
