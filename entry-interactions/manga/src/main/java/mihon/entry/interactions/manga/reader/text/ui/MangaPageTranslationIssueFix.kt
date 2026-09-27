package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Starts the fix for [issue] over the reader: a picker, the engine's download or setup, or [askDisclosure] for a
 * disclosure the user must read first. Only what Android controls goes to [openSettings].
 */
internal fun TranslationSessionHostCoordinator.fixPageTranslation(
    issue: MangaPageTranslationIssue,
    openSettings: () -> Unit,
    askDisclosure: (MangaPageTranslationIssue.DisclosureRequired) -> Unit,
) {
    val action = when (issue) {
        is MangaPageTranslationIssue.SameLanguage,
        MangaPageTranslationIssue.TargetRequired,
        is MangaPageTranslationIssue.UnsupportedPair,
        -> TranslationSessionExternalAction.ChooseTargetLanguage
        is MangaPageTranslationIssue.LanguageDataRequired ->
            TranslationSessionExternalAction.DownloadModels(issue.engine, issue.models)
        is MangaPageTranslationIssue.SetupRequired -> TranslationSessionExternalAction.OpenSetup(issue.engine)
        MangaPageTranslationIssue.EngineChoiceRequired,
        MangaPageTranslationIssue.EngineUnsupported,
        -> TranslationSessionExternalAction.ChooseEngine
        is MangaPageTranslationIssue.DisclosureRequired -> return askDisclosure(issue)
        is MangaPageTranslationIssue.Unavailable -> return openSettings()
        MangaPageTranslationIssue.SetupInProgress -> return
    }
    handleExternalAction(action) {}
}

/** Shows the engine's disclosure and accepts it for translations on the page. */
@Composable
internal fun MangaPageTranslationDisclosureDialog(
    issue: MangaPageTranslationIssue.DisclosureRequired,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(issue.disclosure.title) },
        text = { Text(issue.disclosure.message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(issue.disclosure.confirmationLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
