package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.CoroutineScope
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState

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
) : AutoCloseable {
    val hostCoordinator = TranslationSessionHostCoordinator(
        feature = feature,
        hostActions = hostActions,
        scope = scope,
        selectionSettleDelayMillis = 0,
    )

    fun translate(
        text: String,
        language: LanguageTag,
        pageText: String,
        anchor: TranslationSelectionAnchor?,
    ) {
        hostCoordinator.controller.submit(
            TranslationSessionInput(
                request = TranslationRequest(
                    text = text,
                    sourceLanguage = TranslationSourceLanguageSelection.Explicit(language),
                    languageContext = TextLanguageResolutionContext(
                        surroundingText = pageText.takeIf(String::isNotBlank),
                        declaredLanguages = listOf(language),
                    ),
                ),
                anchor = anchor,
            ),
        )
    }

    fun updateAnchor(anchor: TranslationSelectionAnchor?) = hostCoordinator.controller.updateAnchor(anchor)

    val isActive: Boolean
        get() = hostCoordinator.controller.state.value is TranslationSessionState.Active

    fun dismiss() = hostCoordinator.controller.dismiss()

    fun onResume() = hostCoordinator.onResume()

    override fun close() = hostCoordinator.close()
}
