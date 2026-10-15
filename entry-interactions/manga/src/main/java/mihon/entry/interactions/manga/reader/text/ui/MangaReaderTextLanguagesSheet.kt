package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextState
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The languages translate mode uses for the series, each opening its picker, and whether the series' downloads are
 * translated in the background; [translateDownloads] is null when they cannot be.
 */
@Composable
internal fun MangaReaderTextLanguagesSheet(
    pageLanguage: String,
    target: String?,
    translateDownloads: Boolean?,
    onChoosePageLanguage: () -> Unit,
    onChooseTarget: () -> Unit,
    onTranslateDownloadsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Sheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = KatariTheme.spacing.l)) {
            SheetHeader(stringResource(MR.strings.reader_text_languages))
            SettingsRow(
                title = stringResource(MR.strings.reader_text_page_language),
                subtitle = pageLanguage,
                onClick = onChoosePageLanguage,
            )
            SettingsRow(
                title = stringResource(MR.strings.translation_translate_to),
                subtitle = target ?: stringResource(MR.strings.translation_choose_target_language),
                onClick = onChooseTarget,
            )
            Text(
                text = stringResource(MR.strings.reader_text_page_language_rereads),
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.rowInset,
                    vertical = KatariTheme.spacing.s,
                ),
                role = TextRole.Supporting,
            )
            if (translateDownloads != null) {
                HorizontalDivider()
                ToggleRow(
                    title = stringResource(MR.strings.reader_text_translate_downloads),
                    subtitle = stringResource(MR.strings.reader_text_translate_downloads_summary),
                    checked = translateDownloads,
                    onCheckedChange = onTranslateDownloadsChange,
                )
            }
        }
    }
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
