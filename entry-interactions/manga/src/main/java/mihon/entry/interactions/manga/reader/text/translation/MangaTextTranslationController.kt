package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.language.TranslationLanguageStore

/**
 * Translates recognized page text through the shared translation session.
 *
 * The recognition language is known, so it is passed as the source language instead of being guessed from short
 * bubble text; the rest of the page serves as context.
 */
internal class MangaTextTranslationController(
    feature: TranslationFeature,
    hostActions: TranslationHostActions,
    scope: CoroutineScope,
    languageStore: TranslationLanguageStore,
) : AutoCloseable {
    val hostCoordinator = TranslationSessionHostCoordinator(
        feature = feature,
        hostActions = hostActions,
        scope = scope,
        selectionSettleDelayMillis = 0,
        languageStore = languageStore,
    )

    /** The page language kept for the series; null follows the language the source declares. */
    val pageLanguage: Flow<LanguageTag?> = hostCoordinator.languages.choices.map { it.source }

    /** Keeps [language] as the series' page language. A translation of text read in the old language is closed. */
    fun choosePageLanguage(language: LanguageTag) {
        dismiss()
        hostCoordinator.languages.selectSource(language)
    }

    fun translate(
        text: String,
        language: LanguageTag,
        pageText: String,
        anchor: TranslationSelectionAnchor?,
    ) {
        hostCoordinator.submit(
            text = text,
            languageContext = TextLanguageResolutionContext(
                surroundingText = pageText.takeIf(String::isNotBlank),
                declaredLanguages = listOf(language),
            ),
            anchor = anchor,
            knownSource = language,
        )
    }

    fun updateAnchor(anchor: TranslationSelectionAnchor?) = hostCoordinator.controller.updateAnchor(anchor)

    val isActive: Boolean
        get() = hostCoordinator.controller.state.value is TranslationSessionState.Active

    fun dismiss() = hostCoordinator.controller.dismiss()

    fun onResume() = hostCoordinator.onResume()

    override fun close() = hostCoordinator.close()
}
