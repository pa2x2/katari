package mihon.translation.ui.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineInspection
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.host.TranslationHostActions

class TranslationSessionEnvironmentController(
    private val hostActions: TranslationHostActions,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val initialInspection = TranslationEngineInspection(
        engines = hostActions.knownEngines.map { engine ->
            TranslationEngineState(
                engine = engine,
                presentation = null,
                status = TranslationEngineStatus.Checking,
            )
        },
        selectedEngine = hostActions.selectedEngine.get().takeIf { hostActions.selectedEngine.isSet() },
        selectionResolved = false,
    )
    private val mutableInspection = MutableStateFlow(initialInspection)
    val inspection: StateFlow<TranslationEngineInspection> = mutableInspection.asStateFlow()
    private val mutableEngineStates = MutableStateFlow(initialInspection.engines)
    val engineStates: StateFlow<List<TranslationEngineState>> = mutableEngineStates.asStateFlow()

    private val languageSupportController = TranslationLanguageSupportController(hostActions, scope)
    val languageSupport: StateFlow<TranslationLanguageSupportState> = languageSupportController.state

    private var engineRefreshJob: Job? = null

    init {
        refreshEngineStates()
    }

    fun refreshEngineStates() {
        engineRefreshJob?.cancel()
        engineRefreshJob = scope.launch {
            hostActions.inspectEngineStates().collect { inspection ->
                mutableInspection.value = inspection
                mutableEngineStates.value = inspection.engines
            }
        }
    }

    fun loadLanguageSupport(engine: TranslationEngineId?) {
        languageSupportController.load(engine)
    }

    fun retryLanguageSupport() {
        languageSupportController.retry()
    }

    override fun close() {
        engineRefreshJob?.cancel()
        engineRefreshJob = null
        languageSupportController.clear()
    }
}
