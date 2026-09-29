package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag

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
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shortcuts, key = { it.tag.value }) { option ->
                FilterChip(
                    selected = option.tag == selected,
                    onClick = { onSelect(option.tag) },
                    label = { Text(option.nativeName) },
                )
            }
        }
    }
}
