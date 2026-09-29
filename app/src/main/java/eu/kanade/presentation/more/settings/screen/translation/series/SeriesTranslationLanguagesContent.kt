package eu.kanade.presentation.more.settings.screen.translation.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.entry.components.EntryCover
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/** Series that use their own translation languages, each of which can go back to the defaults. */
@Composable
internal fun SeriesTranslationLanguagesContent(
    series: List<SeriesTranslationLanguages>?,
    onBack: () -> Unit,
    onClear: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    var confirmingClearAll by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.translation_series_languages),
                navigateUp = onBack,
                actions = {
                    if (!series.isNullOrEmpty()) {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.translation_series_languages_clear_all),
                                    icon = Icons.Outlined.DeleteSweep,
                                    onClick = { confirmingClearAll = true },
                                ),
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        when {
            series == null -> LoadingScreen(Modifier.padding(contentPadding))
            series.isEmpty() -> EmptyScreen(
                stringRes = MR.strings.translation_series_languages_none,
                modifier = Modifier.padding(contentPadding),
            )
            else -> ScrollbarLazyColumn(contentPadding = contentPadding) {
                items(series, key = SeriesTranslationLanguages::entryId) { item ->
                    SeriesTranslationLanguagesRow(item, onClear = { onClear(item.entryId) })
                }
            }
        }
    }
    if (confirmingClearAll) {
        AlertDialog(
            onDismissRequest = { confirmingClearAll = false },
            title = { Text(stringResource(MR.strings.translation_series_languages_clear_all_title)) },
            text = { Text(stringResource(MR.strings.translation_series_languages_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingClearAll = false
                        onClearAll()
                    },
                ) {
                    Text(stringResource(MR.strings.translation_series_languages_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClearAll = false }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SeriesTranslationLanguagesRow(
    item: SeriesTranslationLanguages,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(64.dp)
            .padding(
                start = MaterialTheme.padding.medium,
                top = MaterialTheme.padding.small,
                bottom = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Square(data = item.cover, modifier = Modifier.fillMaxHeight())
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.padding.medium),
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            TranslationDirectionText(
                text = seriesLanguagesSummary(item),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onClear) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(MR.strings.translation_series_languages_clear),
            )
        }
    }
}

/** Both languages as a pair, or the one the series keeps. */
@Composable
private fun seriesLanguagesSummary(item: SeriesTranslationLanguages): String {
    val content = item.contentLanguage
    val target = item.targetLanguage
    return when {
        content != null && target != null ->
            stringResource(MR.strings.translation_language_pair, content.displayName(), target.displayName())
        target != null -> stringResource(MR.strings.translation_series_languages_target_only, target.displayName())
        content != null -> stringResource(MR.strings.translation_series_languages_content_only, content.displayName())
        else -> ""
    }
}
