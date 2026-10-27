package mihon.translation.api.preparation

import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.ResolvedTranslationRoute

/**
 * Whether text in a known language could be translated before any text exists, for example to settle prerequisites
 * before work is scheduled. Only [Ready] differs from text preparation; every other outcome is a
 * [TranslationRequirement].
 */
sealed interface TranslationRoutePreparation {
    /** Text would be translated along [route]; name its languages and engine in later requests to use exactly it. */
    data class Ready(
        val route: ResolvedTranslationRoute,
        val presentation: TranslationProviderPresentation,
    ) : TranslationRoutePreparation
}
