package eu.kanade.presentation.more.settings.screen.translation.series

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.DeleteSweep
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

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
                                    icon = MaterialSymbols.Rounded.DeleteSweep,
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
                rowGroupItems(series, key = SeriesTranslationLanguages::entryId) { item ->
                    SeriesTranslationLanguagesRow(item, onClear = { onClear(item.entryId) })
                }
            }
        }
    }
    if (confirmingClearAll) {
        ConfirmDialog(
            title = stringResource(MR.strings.translation_series_languages_clear_all_title),
            text = stringResource(MR.strings.translation_series_languages_clear_all_message),
            confirmLabel = stringResource(MR.strings.translation_series_languages_clear_all),
            onConfirm = {
                confirmingClearAll = false
                onClearAll()
            },
            onDismissRequest = { confirmingClearAll = false },
            destructive = true,
        )
    }
}

@Composable
private fun SeriesTranslationLanguagesRow(
    item: SeriesTranslationLanguages,
    onClear: () -> Unit,
) {
    SettingsRow(
        title = item.title,
        leading = {
            EntryCover.Square(
                data = item.cover,
                modifier = Modifier.size(KatariTheme.sizes.artwork),
                frame = CoverFrame.Thumbnail,
            )
        },
        trailing = {
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.translation_series_languages_clear),
                onClick = onClear,
            )
        },
        below = {
            TranslationDirectionText(
                text = seriesLanguagesSummary(item),
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
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
