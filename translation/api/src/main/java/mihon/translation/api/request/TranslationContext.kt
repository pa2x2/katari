package mihon.translation.api.request

/**
 * What surrounds a text and may change how it should be translated.
 *
 * Callers pass everything they know. An engine is only ever given the elements it reads, so a caller never has to
 * know which engine will translate.
 *
 * @property work the work the text belongs to.
 * @property precedingText text that comes before the translated text in the same work, in reading order.
 */
data class TranslationContext(
    val work: TranslationWorkContext? = null,
    val precedingText: List<String> = emptyList(),
) {
    companion object {
        val None = TranslationContext()
    }
}

data class TranslationWorkContext(
    val title: String,
    val description: String? = null,
)
