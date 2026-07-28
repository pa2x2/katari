package mihon.translation.ui.session

import mihon.translation.api.TranslationExecution
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationResult

data class TranslationSelectionAnchor(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class TranslationSessionInput(
    val request: TranslationRequest,
    val anchor: TranslationSelectionAnchor? = null,
)

sealed interface TranslationSessionState {
    data object Hidden : TranslationSessionState

    sealed interface Active : TranslationSessionState {
        val input: TranslationSessionInput
    }

    data class Settling(
        override val input: TranslationSessionInput,
        val previousResult: TranslationResult? = null,
    ) : Active

    data class Preparing(
        override val input: TranslationSessionInput,
        val previousResult: TranslationResult? = null,
    ) : Active

    data class Ready(
        override val input: TranslationSessionInput,
        val preparation: TranslationPreparation.Ready,
    ) : Active

    data class Translating(
        override val input: TranslationSessionInput,
        val presentation: TranslationProviderPresentation,
        val previousResult: TranslationResult? = null,
    ) : Active

    data class PreparationRequired(
        override val input: TranslationSessionInput,
        val preparation: TranslationPreparation,
    ) : Active {
        init {
            require(preparation !is TranslationPreparation.Ready)
        }
    }

    data class Success(
        override val input: TranslationSessionInput,
        val result: TranslationResult,
    ) : Active

    data class Failed(
        override val input: TranslationSessionInput,
        val failure: TranslationSessionFailure,
        val presentation: TranslationProviderPresentation? = null,
    ) : Active
}

sealed interface TranslationSessionFailure {
    data object UnexpectedPreparationFailure : TranslationSessionFailure

    data object PreparationTimedOut : TranslationSessionFailure

    data class ExecutionFailure(
        val execution: TranslationExecution.Failed,
    ) : TranslationSessionFailure

    data object ExecutionTimedOut : TranslationSessionFailure

    data object UnexpectedExecutionFailure : TranslationSessionFailure
}

internal fun TranslationSessionState.resultForRefresh(): TranslationResult? {
    return when (this) {
        is TranslationSessionState.Success -> result
        is TranslationSessionState.Settling -> previousResult
        is TranslationSessionState.Preparing -> previousResult
        is TranslationSessionState.Translating -> previousResult
        TranslationSessionState.Hidden,
        is TranslationSessionState.Ready,
        is TranslationSessionState.PreparationRequired,
        is TranslationSessionState.Failed,
        -> null
    }
}

internal fun TranslationSessionState.withInput(
    input: TranslationSessionInput,
): TranslationSessionState {
    return when (this) {
        TranslationSessionState.Hidden -> this
        is TranslationSessionState.Settling -> copy(input = input)
        is TranslationSessionState.Preparing -> copy(input = input)
        is TranslationSessionState.Ready -> copy(input = input)
        is TranslationSessionState.Translating -> copy(input = input)
        is TranslationSessionState.PreparationRequired -> copy(input = input)
        is TranslationSessionState.Success -> copy(input = input)
        is TranslationSessionState.Failed -> copy(input = input)
    }
}
