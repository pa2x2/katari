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

    /** Nothing more is translated until the user resolves [requirement]; the batch ends here. */
    data class Blocked(
        val requirement: TranslationRequirement,
    ) : TranslationBatchUpdate
}
