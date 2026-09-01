package mihon.translation.ui.session

import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationResult

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

data class TranslationSessionResult(
    val input: TranslationSessionInput,
    val result: TranslationResult,
)

sealed interface TranslationSessionState {
    data object Hidden : TranslationSessionState

    sealed interface Active : TranslationSessionState {
        val input: TranslationSessionInput
    }

    data class Settling(
        override val input: TranslationSessionInput,
        val previousResult: TranslationSessionResult? = null,
    ) : Active

    data class Preparing(
        override val input: TranslationSessionInput,
        val previousResult: TranslationSessionResult? = null,
    ) : Active

    data class Ready(
        override val input: TranslationSessionInput,
        val preparation: TranslationPreparation.Ready,
        val previousResult: TranslationSessionResult? = null,
    ) : Active

    data class Translating(
        override val input: TranslationSessionInput,
        val presentation: TranslationProviderPresentation,
        val previousResult: TranslationSessionResult? = null,
    ) : Active

    data class PreparationRequired(
        override val input: TranslationSessionInput,
        val preparation: TranslationPreparation,
        val previousResult: TranslationSessionResult? = null,
    ) : Active {
        init {
            require(preparation !is TranslationPreparation.Ready)
        }
    }

    data class Success(
        override val input: TranslationSessionInput,
        val result: TranslationResult,
    ) : Active

    data class ProviderSurfaceOpened(
        override val input: TranslationSessionInput,
        val presentation: TranslationProviderPresentation,
        val previousResult: TranslationSessionResult? = null,
    ) : Active

    data class Failed(
        override val input: TranslationSessionInput,
        val failure: TranslationSessionFailure,
        val presentation: TranslationProviderPresentation? = null,
        val previousResult: TranslationSessionResult? = null,
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

fun TranslationSessionState.displayedSessionResult(): TranslationSessionResult? {
    return when (this) {
        is TranslationSessionState.Success -> TranslationSessionResult(input, result)
        is TranslationSessionState.Settling -> previousResult
        is TranslationSessionState.Preparing -> previousResult
        is TranslationSessionState.Translating -> previousResult
        is TranslationSessionState.Ready -> previousResult
        is TranslationSessionState.PreparationRequired -> previousResult
        is TranslationSessionState.ProviderSurfaceOpened -> previousResult
        is TranslationSessionState.Failed -> previousResult
        TranslationSessionState.Hidden,
        -> null
    }
}

fun TranslationSessionState.displayedResult(): TranslationResult? = displayedSessionResult()?.result

fun TranslationSessionState.isTranslationInProgress(): Boolean =
    this is TranslationSessionState.Settling ||
        this is TranslationSessionState.Preparing ||
        this is TranslationSessionState.Translating

internal fun TranslationSessionState.resultForRefresh(): TranslationSessionResult? = displayedSessionResult()

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
        is TranslationSessionState.ProviderSurfaceOpened -> copy(input = input)
        is TranslationSessionState.Failed -> copy(input = input)
    }
}
