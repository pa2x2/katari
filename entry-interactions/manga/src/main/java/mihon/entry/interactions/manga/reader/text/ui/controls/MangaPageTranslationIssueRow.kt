package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What keeps translations off the page, with the button that fixes it. */
@Composable
internal fun MangaPageTranslationIssueRow(
    issue: MangaPageTranslationIssue,
    onFix: (MangaPageTranslationIssue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        TranslationDirectionText(
            text = mangaPageTranslationIssueMessage(issue),
            role = TextRole.Body,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = KatariTheme.spacing.m),
        )
        mangaPageTranslationIssueAction(issue)?.let { label ->
            Button(label = label, onClick = { onFix(issue) }, style = ButtonStyle.Text)
        }
    }
}

@Composable
internal fun mangaPageTranslationIssueMessage(issue: MangaPageTranslationIssue): String = when (issue) {
    is MangaPageTranslationIssue.SameLanguage ->
        stringResource(MR.strings.reader_text_translation_same_language, issue.language.displayName())
    MangaPageTranslationIssue.TargetRequired -> stringResource(MR.strings.reader_text_translation_target_required)
    is MangaPageTranslationIssue.UnsupportedPair ->
        issue.engineName
            ?.let {
                stringResource(
                    MR.strings.reader_text_translation_unsupported_pair,
                    it,
                    issue.source.displayName(),
                    issue.target.displayName(),
                )
            }
            ?: stringResource(
                MR.strings.reader_text_translation_unsupported_pair_unnamed,
                issue.source.displayName(),
                issue.target.displayName(),
            )
    is MangaPageTranslationIssue.LanguageDataRequired ->
        stringResource(MR.strings.reader_text_translation_language_data, issue.engineName)
    is MangaPageTranslationIssue.SetupRequired -> when (val reason = issue.reason) {
        TranslationSystemSetupReason.ServiceDisabled -> stringResource(MR.strings.translation_service_disabled)
        TranslationSystemSetupReason.LanguageModelsRequired ->
            stringResource(MR.strings.reader_text_translation_language_data, issue.engineName)
        is TranslationSystemSetupReason.ProviderActionRequired -> reason.description
    }
    is MangaPageTranslationIssue.DisclosureRequired ->
        stringResource(MR.strings.reader_text_translation_not_set_up, issue.engineName)
    MangaPageTranslationIssue.SetupInProgress -> stringResource(MR.strings.translation_setup_in_progress)
    MangaPageTranslationIssue.EngineChoiceRequired -> stringResource(MR.strings.reader_text_translation_engine_required)
    MangaPageTranslationIssue.EngineUnsupported -> stringResource(MR.strings.reader_text_translation_surface)
    is MangaPageTranslationIssue.EngineFailed -> issue.message ?: stringResource(MR.strings.translation_failed)
    is MangaPageTranslationIssue.Unavailable -> when (val reason = issue.reason) {
        is TranslationUnavailableReason.UnsupportedOs ->
            stringResource(MR.strings.translation_unsupported_os, reason.minimumApi)
        TranslationUnavailableReason.ServiceMissing -> stringResource(MR.strings.translation_service_missing)
        TranslationUnavailableReason.SystemSettingsUnavailable ->
            stringResource(MR.strings.translation_settings_unavailable)
        is TranslationUnavailableReason.UnsupportedLanguage ->
            stringResource(MR.strings.translation_unsupported_language, reason.language.displayName())
        is TranslationUnavailableReason.UnsupportedLanguagePair -> stringResource(
            MR.strings.translation_unsupported_pair,
            reason.source.displayName(),
            reason.target.displayName(),
        )
        is TranslationUnavailableReason.EngineUnavailable ->
            stringResource(MR.strings.translation_engine_unavailable, reason.engine.value, reason.reason)
    }
}

@Composable
private fun mangaPageTranslationIssueAction(issue: MangaPageTranslationIssue): String? = when (issue) {
    is MangaPageTranslationIssue.SameLanguage,
    MangaPageTranslationIssue.TargetRequired,
    -> stringResource(MR.strings.reader_text_choose_language)
    is MangaPageTranslationIssue.UnsupportedPair -> stringResource(MR.strings.reader_text_translation_change)
    is MangaPageTranslationIssue.LanguageDataRequired -> stringResource(MR.strings.action_download)
    is MangaPageTranslationIssue.SetupRequired -> when (issue.reason) {
        TranslationSystemSetupReason.ServiceDisabled -> stringResource(MR.strings.action_settings)
        TranslationSystemSetupReason.LanguageModelsRequired -> stringResource(MR.strings.action_download)
        is TranslationSystemSetupReason.ProviderActionRequired -> stringResource(
            MR.strings.reader_text_translation_set_up,
        )
    }
    is MangaPageTranslationIssue.DisclosureRequired -> stringResource(MR.strings.reader_text_translation_set_up)
    MangaPageTranslationIssue.SetupInProgress -> null
    MangaPageTranslationIssue.EngineChoiceRequired,
    MangaPageTranslationIssue.EngineUnsupported,
    -> stringResource(MR.strings.reader_text_translation_choose)
    is MangaPageTranslationIssue.EngineFailed -> stringResource(MR.strings.action_retry)
    is MangaPageTranslationIssue.Unavailable -> stringResource(MR.strings.action_settings)
}
