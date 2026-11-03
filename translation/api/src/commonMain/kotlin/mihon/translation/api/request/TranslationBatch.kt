package mihon.translation.api.request

/**
 * Texts that belong together, such as the speech bubbles of one page in reading order, translated along a [route]
 * that [mihon.translation.api.TranslationFeature.prepareRoute] found ready.
 *
 * An engine that reads context translates the [segments] together, each in light of the others and of [context].
 */
data class TranslationBatch(
    val route: ResolvedTranslationRoute,
    val segments: List<String>,
    val context: TranslationContext = TranslationContext.None,
)
