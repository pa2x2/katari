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
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One-tap candidates for the most recently used languages, rendered above the full picker list.
 *
 * The row is pre-filtered by the caller to languages the active engine currently supports.
 */
@Composable
fun TranslationLanguageRecentsRow(
    recents: List<TranslationLanguageOption>,
    selected: LanguageTag?,
    onSelect: (LanguageTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(MR.strings.translation_recent_languages),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recents, key = { it.tag.value }) { option ->
                FilterChip(
                    selected = option.tag == selected,
                    onClick = { onSelect(option.tag) },
                    label = { Text(option.nativeName) },
                )
            }
        }
    }
}
