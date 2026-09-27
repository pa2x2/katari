package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.components.AdaptiveSheet
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextState
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The languages translate mode uses for the series, each opening its picker. */
@Composable
internal fun MangaReaderTextLanguagesSheet(
    pageLanguage: String,
    target: String?,
    onChoosePageLanguage: () -> Unit,
    onChooseTarget: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.reader_text_languages),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(MR.strings.action_close))
                }
            }
            HorizontalDivider()
            LanguageRow(
                label = stringResource(MR.strings.reader_text_page_language),
                value = pageLanguage,
                onClick = onChoosePageLanguage,
            )
            LanguageRow(
                label = stringResource(MR.strings.translation_translate_to),
                value = target ?: stringResource(MR.strings.translation_choose_target_language),
                onClick = onChooseTarget,
            )
            Text(
                text = stringResource(MR.strings.reader_text_page_language_rereads),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        supportingContent = { Text(value) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
        content = { Text(label) },
    )
}

/** The language pages are read in, with where it comes from when that is not obvious. */
@Composable
internal fun mangaPageLanguageSummary(state: MangaReaderTextState): String {
    val language = state.language?.displayName() ?: return stringResource(MR.strings.reader_text_page_language_unknown)
    val declared = state.declaredLanguage
    return when {
        !state.languageKept && declared != null -> stringResource(
            MR.strings.reader_text_page_language_from_source,
            language,
        )
        declared != null && declared != state.language ->
            stringResource(MR.strings.reader_text_page_language_source_lists, language, declared.displayName())
        state.languageKept -> stringResource(MR.strings.translation_language_this_series, language)
        else -> language
    }
}
