package mihon.entry.interactions.book.reader.translation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import mihon.entry.interactions.book.R
import mihon.entry.viewer.settings.ui.ReaderSharedSettingDetails
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.language.translationEffectiveTargetSummary
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.NavigationItem
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

/**
 * The book's translation languages, shown with the automatic-translation setting. They also apply to Translate in
 * the selection menu, so the rows stay visible while automatic translation is off. Each row opens the popup's picker.
 */
@Composable
internal fun bookTranslationLanguageDetails(
    coordinator: TranslationSessionHostCoordinator,
    declaredLanguage: LanguageTag?,
): ReaderSharedSettingDetails {
    val choices by coordinator.languages.choices.collectAsState()
    val target = coordinator.languages.effectiveTarget(choices)
    val targetSummary = target?.let { translationEffectiveTargetSummary(it) }
        ?: stringResource(MR.strings.translation_choose_target_language)
    val bookLanguageSummary = bookLanguageSummary(choices.source, declaredLanguage)
    return ReaderSharedSettingDetails(
        summary = target?.let {
            androidStringResource(R.string.book_reader_automatic_translation_target, it.language.displayName())
        },
    ) {
        NavigationItem(
            label = stringResource(MR.strings.translation_translate_to),
            subtitle = targetSummary,
            onClick = { coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseTargetLanguage) {} },
        )
        NavigationItem(
            label = androidStringResource(R.string.book_reader_book_language),
            subtitle = bookLanguageSummary,
            onClick = { coordinator.handleExternalAction(TranslationSessionExternalAction.ChooseSourceLanguage) {} },
        )
    }
}

/** The language kept for the series, otherwise automatic detection helped by what the book is marked as. */
@Composable
private fun bookLanguageSummary(kept: LanguageTag?, declared: LanguageTag?): String = when {
    kept != null -> stringResource(MR.strings.translation_language_this_series, kept.displayName())
    declared != null -> androidStringResource(R.string.book_reader_book_language_declared, declared.displayName())
    else -> stringResource(MR.strings.translation_source_automatic)
}
