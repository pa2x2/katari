package mihon.translation.ui.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageSupport
import mihon.translation.api.TranslationLanguageSupportInspection

class TranslationLanguageSupportController(
    private val hostActions: TranslationHostActions,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    private val mutableState = MutableStateFlow<TranslationLanguageSupportState>(
        TranslationLanguageSupportState.Idle,
    )
    val state: StateFlow<TranslationLanguageSupportState> = mutableState.asStateFlow()

    private var generation = 0L
    private var selectedEngine: TranslationEngineId? = null
    private var inspectionJob: Job? = null
    private var timeoutJob: Job? = null

    init {
        require(timeoutMillis > 0)
    }

    fun load(engine: TranslationEngineId?) {
        selectedEngine = engine
        val operationGeneration = nextGeneration()
        inspectionJob?.cancel()
        timeoutJob?.cancel()
        inspectionJob = null
        timeoutJob = null
        if (engine == null) {
            mutableState.value = TranslationLanguageSupportState.Idle
            return
        }

        mutableState.value = TranslationLanguageSupportState.Loading(engine)
        timeoutJob = scope.launch {
            delay(timeoutMillis)
            if (!isCurrent(operationGeneration, engine)) return@launch
            nextGeneration()
            mutableState.value = TranslationLanguageSupportState.Unavailable(
                engine = engine,
                reason = "Translation languages could not be loaded in time",
            )
            inspectionJob?.cancel()
            inspectionJob = null
        }
        inspectionJob = scope.launch {
            val inspection = try {
                hostActions.inspectLanguageSupport(engine)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                TranslationLanguageSupportInspection.Unavailable(
                    "Translation languages could not be loaded",
                )
            }
            if (!isCurrent(operationGeneration, engine)) return@launch
            timeoutJob?.cancel()
            timeoutJob = null
            mutableState.value = when (inspection) {
                is TranslationLanguageSupportInspection.Available ->
                    TranslationLanguageSupportState.Available(engine, inspection.support)
                is TranslationLanguageSupportInspection.Unavailable ->
                    TranslationLanguageSupportState.Unavailable(engine, inspection.reason)
            }
        }
    }

    fun retry() {
        load(selectedEngine)
    }

    fun clear() {
        selectedEngine = null
        nextGeneration()
        inspectionJob?.cancel()
        timeoutJob?.cancel()
        inspectionJob = null
        timeoutJob = null
        mutableState.value = TranslationLanguageSupportState.Idle
    }

    private fun nextGeneration(): Long {
        generation += 1
        return generation
    }

    private fun isCurrent(
        operationGeneration: Long,
        engine: TranslationEngineId,
    ): Boolean = generation == operationGeneration && selectedEngine == engine

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
    }
}

sealed interface TranslationLanguageSupportState {
    data object Idle : TranslationLanguageSupportState

    data class Loading(
        val engine: TranslationEngineId,
    ) : TranslationLanguageSupportState

    data class Available(
        val engine: TranslationEngineId,
        val support: TranslationLanguageSupport,
    ) : TranslationLanguageSupportState

    data class Unavailable(
        val engine: TranslationEngineId,
        val reason: String? = null,
    ) : TranslationLanguageSupportState
}
