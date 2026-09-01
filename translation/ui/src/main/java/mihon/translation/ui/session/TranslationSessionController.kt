package mihon.translation.ui.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationExecution
import mihon.translation.api.result.TranslationResult

class TranslationSessionController(
    private val feature: TranslationFeature,
    parentScope: CoroutineScope,
    private val executionMode: TranslationSessionExecutionMode =
        TranslationSessionExecutionMode.FollowProviderPolicy,
    private val selectionSettleDelayMillis: Long = DEFAULT_SELECTION_SETTLE_DELAY_MILLIS,
    private val preparationTimeoutMillis: Long = DEFAULT_PREPARATION_TIMEOUT_MILLIS,
    private val executionTimeoutMillis: Long = DEFAULT_EXECUTION_TIMEOUT_MILLIS,
) {
    private val controllerJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + controllerJob)
    private val mutableState = MutableStateFlow<TranslationSessionState>(TranslationSessionState.Hidden)

    val state: StateFlow<TranslationSessionState> = mutableState.asStateFlow()

    private var generation = 0L
    private var activeJob: Job? = null
    private var preparationTimeoutJob: Job? = null
    private var executionTimeoutJob: Job? = null
    private var currentInput: TranslationSessionInput? = null
    private var closed = false

    init {
        require(selectionSettleDelayMillis >= 0)
        require(preparationTimeoutMillis > 0)
        require(executionTimeoutMillis > 0)
        controllerJob.invokeOnCompletion {
            closed = true
            activeJob = null
            preparationTimeoutJob = null
            executionTimeoutJob = null
            currentInput = null
            mutableState.value = TranslationSessionState.Hidden
        }
    }

    fun submit(input: TranslationSessionInput) {
        check(!closed) { "Translation session controller is closed" }
        val current = currentInput
        if (current != null && current.request == input.request) {
            currentInput = input
            mutableState.value = mutableState.value.withInput(input)
            return
        }
        startPreparation(input, selectionSettleDelayMillis)
    }

    fun updateAnchor(anchor: TranslationSelectionAnchor?) {
        val current = currentInput ?: return
        val updated = current.copy(anchor = anchor)
        currentInput = updated
        mutableState.value = mutableState.value.withInput(updated)
    }

    fun selectSourceLanguage(language: LanguageTag) {
        updateRequest {
            copy(sourceLanguage = TranslationSourceLanguageSelection.Explicit(language))
        }
    }

    fun selectTargetLanguage(language: LanguageTag) {
        updateRequest {
            copy(targetLanguage = TranslationTargetLanguageSelection.Explicit(language))
        }
    }

    fun selectLanguages(
        source: LanguageTag,
        target: LanguageTag,
    ) {
        updateRequest {
            copy(
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(source),
                targetLanguage = TranslationTargetLanguageSelection.Explicit(target),
            )
        }
    }

    fun selectEngine(selection: TranslationEngineSelection) {
        updateRequest { copy(engine = selection) }
    }

    fun retry() {
        currentInput?.let { startPreparation(it, delayMillis = 0) }
    }

    fun execute() {
        val ready = mutableState.value as? TranslationSessionState.Ready ?: return
        val operationGeneration = nextGeneration()
        activeJob?.cancel()
        activeJob = scope.launch {
            executeReady(operationGeneration, ready.input, ready.preparation, ready.previousResult)
        }
    }

    fun dismiss() {
        clear()
    }

    fun clear() {
        nextGeneration()
        activeJob?.cancel()
        activeJob = null
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = null
        executionTimeoutJob?.cancel()
        executionTimeoutJob = null
        currentInput = null
        mutableState.value = TranslationSessionState.Hidden
    }

    fun close() {
        if (closed) return
        closed = true
        clear()
        scope.cancel()
    }

    private fun updateRequest(
        transform: TranslationRequest.() -> TranslationRequest,
    ) {
        val current = currentInput ?: return
        startPreparation(current.copy(request = current.request.transform()), delayMillis = 0)
    }

    private fun startPreparation(
        input: TranslationSessionInput,
        delayMillis: Long,
    ) {
        var previousResult = mutableState.value.resultForRefresh()
        val operationGeneration = nextGeneration()
        activeJob?.cancel()
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = null
        executionTimeoutJob?.cancel()
        executionTimeoutJob = null
        currentInput = input
        mutableState.value = TranslationSessionState.Settling(input, previousResult)
        activeJob = scope.launch {
            if (delayMillis > 0) delay(delayMillis)
            if (!isCurrent(operationGeneration)) return@launch

            var firstPreparation = true
            while (isCurrent(operationGeneration)) {
                if (firstPreparation) {
                    mutableState.value = TranslationSessionState.Preparing(input, previousResult)
                }
                val preparation = try {
                    prepareWithinDeadline(operationGeneration, input)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    latestInput(operationGeneration, input)?.let { latestInput ->
                        mutableState.value = TranslationSessionState.Failed(
                            input = latestInput,
                            failure = TranslationSessionFailure.UnexpectedPreparationFailure,
                            previousResult = previousResult,
                        )
                    }
                    return@launch
                }
                if (!isCurrent(operationGeneration)) return@launch
                val latestInput = latestInput(operationGeneration, input) ?: return@launch
                acceptPreparation(
                    operationGeneration = operationGeneration,
                    input = latestInput,
                    preparation = preparation,
                    autoExecuteImmediate = true,
                    previousResult = previousResult,
                )
                if (preparation !is TranslationPreparation.SetupInProgress) return@launch

                previousResult = null
                firstPreparation = false
                delay(SETUP_PROGRESS_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun acceptPreparation(
        operationGeneration: Long,
        input: TranslationSessionInput,
        preparation: TranslationPreparation,
        autoExecuteImmediate: Boolean,
        previousResult: TranslationSessionResult? = null,
    ) {
        when (preparation) {
            is TranslationPreparation.Ready -> {
                mutableState.value = TranslationSessionState.Ready(input, preparation, previousResult)
                if (
                    autoExecuteImmediate &&
                    executionMode == TranslationSessionExecutionMode.FollowProviderPolicy &&
                    preparation.presentation.invocationPolicy == TranslationInvocationPolicy.Immediate
                ) {
                    executeReady(operationGeneration, input, preparation, previousResult)
                }
            }
            else -> {
                mutableState.value = TranslationSessionState.PreparationRequired(input, preparation, previousResult)
            }
        }
    }

    private suspend fun executeReady(
        operationGeneration: Long,
        input: TranslationSessionInput,
        ready: TranslationPreparation.Ready,
        previousResult: TranslationSessionResult? = null,
    ) {
        if (!isCurrent(operationGeneration)) return
        mutableState.value = TranslationSessionState.Translating(
            input = input,
            presentation = ready.presentation,
            previousResult = previousResult,
        )
        val timeoutJob = scope.launch {
            delay(executionTimeoutMillis)
            val latestInput = latestInput(operationGeneration, input) ?: return@launch
            nextGeneration()
            mutableState.value = TranslationSessionState.Failed(
                input = latestInput,
                failure = TranslationSessionFailure.ExecutionTimedOut,
                presentation = ready.presentation,
                previousResult = previousResult,
            )
            activeJob?.cancel()
        }
        executionTimeoutJob?.cancel()
        executionTimeoutJob = timeoutJob
        val execution = try {
            feature.translate(ready.translation)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            latestInput(operationGeneration, input)?.let { latestInput ->
                mutableState.value = TranslationSessionState.Failed(
                    input = latestInput,
                    failure = TranslationSessionFailure.UnexpectedExecutionFailure,
                    presentation = ready.presentation,
                    previousResult = previousResult,
                )
            }
            return
        } finally {
            timeoutJob.cancel()
            if (executionTimeoutJob === timeoutJob) {
                executionTimeoutJob = null
            }
        }
        if (!isCurrent(operationGeneration)) return
        val latestInput = latestInput(operationGeneration, input) ?: return

        when (execution) {
            is TranslationExecution.Success -> {
                mutableState.value = TranslationSessionState.Success(latestInput, execution.result)
            }
            is TranslationExecution.PreparationChanged -> {
                acceptPreparation(
                    operationGeneration = operationGeneration,
                    input = latestInput,
                    preparation = execution.preparation,
                    autoExecuteImmediate = false,
                    previousResult = previousResult,
                )
                if (execution.preparation is TranslationPreparation.SetupInProgress) {
                    pollSetupProgress(operationGeneration, latestInput, previousResult)
                }
            }
            is TranslationExecution.ProviderSurfaceOpened -> {
                mutableState.value = TranslationSessionState.ProviderSurfaceOpened(
                    input = latestInput,
                    presentation = execution.presentation,
                    previousResult = previousResult,
                )
            }
            is TranslationExecution.Failed -> {
                mutableState.value = TranslationSessionState.Failed(
                    input = latestInput,
                    failure = TranslationSessionFailure.ExecutionFailure(execution),
                    presentation = ready.presentation,
                    previousResult = previousResult,
                )
            }
        }
    }

    private suspend fun pollSetupProgress(
        operationGeneration: Long,
        input: TranslationSessionInput,
        previousResult: TranslationSessionResult?,
    ) {
        var lastInput = input
        while (isCurrent(operationGeneration)) {
            delay(SETUP_PROGRESS_POLL_INTERVAL_MILLIS)
            val preparation = try {
                prepareWithinDeadline(operationGeneration, lastInput)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                latestInput(operationGeneration, lastInput)?.let { latestInput ->
                    mutableState.value = TranslationSessionState.Failed(
                        input = latestInput,
                        failure = TranslationSessionFailure.UnexpectedPreparationFailure,
                        previousResult = previousResult,
                    )
                }
                return
            }
            if (!isCurrent(operationGeneration)) return
            lastInput = latestInput(operationGeneration, lastInput) ?: return
            acceptPreparation(
                operationGeneration,
                lastInput,
                preparation,
                autoExecuteImmediate = true,
                previousResult = previousResult,
            )
            if (preparation !is TranslationPreparation.SetupInProgress) return
        }
    }

    private suspend fun prepareWithinDeadline(
        operationGeneration: Long,
        input: TranslationSessionInput,
    ): TranslationPreparation {
        val timeoutJob = scope.launch {
            delay(preparationTimeoutMillis)
            val latestInput = latestInput(operationGeneration, input) ?: return@launch
            val previousResult = mutableState.value.resultForRefresh()
            nextGeneration()
            mutableState.value = TranslationSessionState.Failed(
                input = latestInput,
                failure = TranslationSessionFailure.PreparationTimedOut,
                previousResult = previousResult,
            )
            activeJob?.cancel()
        }
        preparationTimeoutJob?.cancel()
        preparationTimeoutJob = timeoutJob
        return try {
            feature.prepare(input.request)
        } finally {
            timeoutJob.cancel()
            if (preparationTimeoutJob === timeoutJob) {
                preparationTimeoutJob = null
            }
        }
    }

    private fun nextGeneration(): Long {
        generation += 1
        return generation
    }

    private fun isCurrent(operationGeneration: Long): Boolean {
        return generation == operationGeneration
    }

    private fun latestInput(
        operationGeneration: Long,
        fallback: TranslationSessionInput,
    ): TranslationSessionInput? {
        if (!isCurrent(operationGeneration)) return null
        return currentInput
            ?.takeIf { it.request == fallback.request }
            ?: fallback
    }

    private companion object {
        const val DEFAULT_SELECTION_SETTLE_DELAY_MILLIS = 250L
        const val DEFAULT_PREPARATION_TIMEOUT_MILLIS = 15_000L
        const val DEFAULT_EXECUTION_TIMEOUT_MILLIS = 30_000L
        const val SETUP_PROGRESS_POLL_INTERVAL_MILLIS = 1_000L
    }
}

enum class TranslationSessionExecutionMode {
    FollowProviderPolicy,
    AwaitUserAction,
}
