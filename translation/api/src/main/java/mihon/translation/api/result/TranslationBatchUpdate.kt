package mihon.translation.api.result

import mihon.translation.api.preparation.TranslationRequirement

/** Progress of a [mihon.translation.api.request.TranslationBatch]; every segment is reported at most once. */
sealed interface TranslationBatchUpdate {
    /** The segment at [index] of the batch was translated. */
    data class Translated(
        val index: Int,
        val text: String,
    ) : TranslationBatchUpdate {
        init {
            require(text.isNotBlank())
        }
    }

    /** The segment at [index] could not be translated; the others still can be. */
    data class Failed(
        val index: Int,
    ) : TranslationBatchUpdate

    /**
     * The engine failed to translate, for the reason in [failure] when it gave one. Segments not reported by now stay
     * untranslated and the batch ends here; unlike after [Blocked], translating it again later may succeed as it is.
     */
    data class EngineFailed(
        val failure: TranslationFailureReason.ProviderFailure,
    ) : TranslationBatchUpdate

    /** Nothing more is translated until the user resolves [requirement]; the batch ends here. */
    data class Blocked(
        val requirement: TranslationRequirement,
    ) : TranslationBatchUpdate
}
