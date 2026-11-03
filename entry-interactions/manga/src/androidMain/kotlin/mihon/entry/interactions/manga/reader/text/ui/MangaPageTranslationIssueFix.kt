package mihon.entry.interactions.manga.reader.text.ui

import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.TranslationSessionHostCoordinator

/**
 * Starts the fix for [issue] over the reader: a picker, the engine's download or setup, [askDisclosure] for a
 * disclosure the user must read first, or [retry] after the engine failed. Only what Android controls goes to
 * [openSettings].
 */
internal fun TranslationSessionHostCoordinator.fixPageTranslation(
    issue: MangaPageTranslationIssue,
    openSettings: () -> Unit,
    askDisclosure: (MangaPageTranslationIssue.DisclosureRequired) -> Unit,
    retry: () -> Unit,
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
        is MangaPageTranslationIssue.EngineFailed -> return retry()
        is MangaPageTranslationIssue.Unavailable -> return openSettings()
        MangaPageTranslationIssue.SetupInProgress -> return
    }
    handleExternalAction(action) {}
}
