package mihon.tts.ui.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.engine.TtsEngineSelection
import mihon.tts.api.engine.TtsEngineState
import mihon.tts.api.engine.TtsEngineStatus
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsHostActions
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameterSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsProcessingPolicy
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection

class TtsSettingsController(
    private val feature: TtsFeature,
    private val hostActions: TtsHostActions,
    private val scope: CoroutineScope,
    initialPreviewLanguage: LanguageTag,
) : AutoCloseable {
    private var savedEngine = hostActions.selectedEngine.get().takeIf { hostActions.selectedEngine.isSet() }
    private var savedDefaultVoice = hostActions.selectedDefaultVoice()
    private var savedVoiceOverrides = hostActions.selectedVoiceOverrides()
    private var savedPitch = hostActions.pitch.get()
    private var savedAllowNetworkVoices = hostActions.allowNetworkVoices.get()
    private val mutableState = MutableStateFlow(
        TtsSettingsState(
            engineInspection = TtsEngineInspection(
                engines = hostActions.knownEngines.map { engine ->
                    TtsEngineState(engine, presentation = null, status = TtsEngineStatus.Checking)
                },
                selectedEngine = null,
                selectionResolved = false,
            ),
            voiceCatalog = TtsVoiceCatalogState.NoEngine,
            voiceOverrides = savedVoiceOverrides,
            speechRate = hostActions.speechRate.get(),
            pitch = hostActions.pitch.get(),
            allowNetworkVoices = savedAllowNetworkVoices,
            previewLanguage = initialPreviewLanguage,
            defaultVoice = savedDefaultVoice,
        ),
    )
    val state = mutableState.asStateFlow()

    private var engineRefreshJob: Job? = null
    private var voiceRefreshJob: Job? = null
    private var engineRefreshGeneration = 0L
    private var voiceRefreshGeneration = 0L
    private var closed = false
    private val previewPlayback = TtsPreviewPlaybackController(
        feature = feature,
        scope = scope,
        currentState = { mutableState.value.preview },
        updateState = { preview ->
            mutableState.update {
                it.copy(
                    preview = preview,
                    previewVoice = it.previewVoice.takeIf {
                        preview == TtsPreviewState.Preparing || preview == TtsPreviewState.Speaking
                    },
                )
            }
        },
    )
    private val preferenceJobs = listOf(
        hostActions.speechRate.changes().onEach { value ->
            mutableState.update { it.copy(speechRate = value) }
        }.launchIn(scope),
        hostActions.pitch.changes().onEach { value ->
            mutableState.update { current ->
                if (current.hasUnsavedProfileChanges) current else current.copy(pitch = value)
            }
        }.launchIn(scope),
        hostActions.allowNetworkVoices.changes().onEach { value ->
            mutableState.update { current ->
                if (current.hasUnsavedProfileChanges) {
                    current
                } else {
                    savedAllowNetworkVoices = value
                    current.copy(allowNetworkVoices = value)
                }
            }
        }.launchIn(scope),
    )

    init {
        refresh()
    }

    fun refresh(forceVoiceRefresh: Boolean = false) {
        if (closed) return
        engineRefreshJob?.cancel()
        val activeGeneration = ++engineRefreshGeneration
        engineRefreshJob = scope.launch {
            hostActions.inspectEngineStates().collect { inspection ->
                if (closed || activeGeneration != engineRefreshGeneration) return@collect
                val previousEngine = mutableState.value.selectedEngine
                val previousEngineStatus = mutableState.value.selectedEngineState?.status
                mutableState.update { current ->
                    if (current.hasUnsavedProfileChanges) {
                        current.copy(
                            engineInspection = inspection.copy(selectedEngine = current.selectedEngine),
                        )
                    } else {
                        savedEngine = inspection.selectedEngine
                        savedDefaultVoice = hostActions.selectedDefaultVoice()
                        savedVoiceOverrides = hostActions.selectedVoiceOverrides()
                        savedPitch = hostActions.pitch.get()
                        savedAllowNetworkVoices = hostActions.allowNetworkVoices.get()
                        current.copy(
                            engineInspection = inspection,
                            defaultVoice = savedDefaultVoice,
                            voiceOverrides = savedVoiceOverrides,
                            pitch = savedPitch,
                            allowNetworkVoices = savedAllowNetworkVoices,
                        )
                    }
                }
                val currentEngine = mutableState.value.selectedEngine
                val currentEngineStatus = mutableState.value.selectedEngineState?.status
                if (currentEngineStatus != TtsEngineStatus.Ready) stopPreview()
                if (forceVoiceRefresh ||
                    previousEngine != currentEngine ||
                    previousEngineStatus != currentEngineStatus ||
                    voiceCatalogNeedsRefresh(currentEngine)
                ) {
                    refreshVoices(currentEngine)
                }
            }
        }
    }

    fun selectEngine(engine: TtsEngineId) {
        val selectable = mutableState.value.engineInspection.engines.any { state ->
            state.engine.id == engine && state.status == TtsEngineStatus.Ready
        }
        if (!selectable) return
        hostActions.setSelectedEngine(engine)
        stopPreview()
        refresh()
    }

    fun selectDraftEngine(engine: TtsEngineId) {
        val selectable = mutableState.value.engineInspection.engines.any { state ->
            state.engine.id == engine && state.status == TtsEngineStatus.Ready
        }
        if (!selectable || mutableState.value.selectedEngine == engine) return
        stopPreview()
        val returningToSavedEngine = engine == savedEngine
        val pitch = if (returningToSavedEngine) {
            savedPitch
        } else {
            when (
                val support = mutableState.value.engineInspection.engines
                    .single { it.engine.id == engine }
                    .capabilities
                    ?.pitch
            ) {
                is TtsParameterSupport.Supported -> support.range.default
                TtsParameterSupport.Unsupported,
                null,
                -> 1f
            }
        }
        mutableState.update { current ->
            current.copy(
                engineInspection = current.engineInspection.copy(selectedEngine = engine),
                voiceCatalog = TtsVoiceCatalogState.Loading(engine),
                defaultVoice = if (returningToSavedEngine) {
                    savedDefaultVoice
                } else {
                    TtsDefaultVoiceSelection.EngineDefault
                },
                voiceOverrides = if (returningToSavedEngine) savedVoiceOverrides else emptyMap(),
                pitch = pitch,
            ).withUnsavedState()
        }
        refreshVoices(engine)
    }

    fun setSpeechRate(value: Float) {
        val support = mutableState.value.selectedEngineState?.capabilities?.speechRate
        if (!support.accepts(value)) return
        stopPreview()
        hostActions.speechRate.set(value)
    }

    fun resetSpeechRate() {
        val default =
            (mutableState.value.selectedEngineState?.capabilities?.speechRate as? TtsParameterSupport.Supported)
                ?.range
                ?.default
                ?: 1f
        setSpeechRate(default)
    }

    fun setPitch(value: Float) {
        val support = mutableState.value.selectedEngineState?.capabilities?.pitch
        if (!support.accepts(value)) return
        stopPreview()
        hostActions.pitch.set(value)
    }

    fun resetPitch() {
        val default = (mutableState.value.selectedEngineState?.capabilities?.pitch as? TtsParameterSupport.Supported)
            ?.range
            ?.default
            ?: 1f
        setPitch(default)
    }

    fun setDraftPitch(value: Float) {
        val support = mutableState.value.selectedEngineState?.capabilities?.pitch
        if (!support.accepts(value)) return
        stopPreview()
        mutableState.update { it.copy(pitch = value).withUnsavedState() }
        toggleConfiguredPreview()
    }

    fun setAllowNetworkVoices(allow: Boolean) {
        stopPreview()
        hostActions.allowNetworkVoices.set(allow)
    }

    fun setVoiceOverride(language: LanguageTag, voice: TtsVoiceId?) {
        val available = (mutableState.value.voiceCatalog as? TtsVoiceCatalogState.Available)?.voices.orEmpty()
        val compatibleVoiceIds = available.compatibleWith(language).mapTo(mutableSetOf(), TtsVoice::id)
        if (voice != null && voice !in compatibleVoiceIds) return
        if (
            voice != null &&
            !mutableState.value.allowNetworkVoices &&
            available.single { it.id == voice }.processing == TtsVoiceProcessing.NetworkRequired
        ) {
            return
        }
        stopPreview()
        hostActions.setSelectedVoice(language, voice)
        mutableState.update { it.copy(voiceOverrides = hostActions.selectedVoiceOverrides()) }
    }

    fun setDraftDefaultVoice(
        selection: TtsDefaultVoiceSelection,
        networkVoiceConfirmed: Boolean = false,
    ) {
        val voice = resolvedVoice(selection) ?: return
        val allowNetworkVoices = mutableState.value.allowNetworkVoices ||
            (voice.processing == TtsVoiceProcessing.NetworkRequired && networkVoiceConfirmed)
        if (voice.processing == TtsVoiceProcessing.NetworkRequired && !allowNetworkVoices) return
        stopPreview()
        mutableState.update {
            it.copy(
                defaultVoice = selection,
                allowNetworkVoices = allowNetworkVoices,
            ).withUnsavedState()
        }
    }

    fun setDraftVoiceOverride(
        language: LanguageTag,
        voice: TtsVoiceId,
        networkVoiceConfirmed: Boolean = false,
    ) {
        val available = (mutableState.value.voiceCatalog as? TtsVoiceCatalogState.Available)?.voices.orEmpty()
        val selectedVoice = available.compatibleWith(language).singleOrNull { it.id == voice } ?: return
        val allowNetworkVoices = mutableState.value.allowNetworkVoices ||
            (selectedVoice.processing == TtsVoiceProcessing.NetworkRequired && networkVoiceConfirmed)
        if (selectedVoice.processing == TtsVoiceProcessing.NetworkRequired && !allowNetworkVoices) return
        stopPreview()
        mutableState.update { current ->
            current.copy(
                voiceOverrides = current.voiceOverrides + (language to voice),
                allowNetworkVoices = allowNetworkVoices,
            ).withUnsavedState()
        }
    }

    fun removeDraftVoiceOverride(language: LanguageTag) {
        stopPreview()
        mutableState.update { current ->
            current.copy(voiceOverrides = current.voiceOverrides - language).withUnsavedState()
        }
    }

    fun setPreviewLanguage(language: LanguageTag) {
        val voices = (mutableState.value.voiceCatalog as? TtsVoiceCatalogState.Available)?.voices.orEmpty()
        if (!voices.supports(language, mutableState.value.allowNetworkVoices)) return
        stopPreview()
        mutableState.update { it.copy(previewLanguage = language) }
    }

    fun togglePreview() {
        previewPlayback.toggle(mutableState.value.previewLanguage)
    }

    fun toggleConfiguredPreview() {
        val state = mutableState.value
        val voice = resolvedVoice(state.defaultVoice) ?: return
        toggleVoicePreview(voice)
    }

    fun auditionVoice(voice: TtsVoice) {
        val state = mutableState.value
        val previewActive = state.preview == TtsPreviewState.Preparing || state.preview == TtsPreviewState.Speaking
        if (previewActive && state.previewVoice == voice.id) {
            stopPreview()
        } else {
            playVoicePreview(voice)
        }
    }

    fun saveProfileChanges() {
        val state = mutableState.value
        val engine = state.selectedEngine ?: return
        if (!state.hasUnsavedProfileChanges || !configurationReady(state)) return
        hostActions.setSelectedEngine(engine)
        hostActions.setSelectedDefaultVoice(state.defaultVoice)
        (savedVoiceOverrides.keys - state.voiceOverrides.keys).forEach { language ->
            hostActions.setSelectedVoice(language, null)
        }
        state.voiceOverrides.forEach(hostActions::setSelectedVoice)
        hostActions.pitch.set(state.pitch)
        hostActions.allowNetworkVoices.set(state.allowNetworkVoices)
        savedEngine = engine
        savedDefaultVoice = state.defaultVoice
        savedVoiceOverrides = state.voiceOverrides
        savedPitch = state.pitch
        savedAllowNetworkVoices = state.allowNetworkVoices
        mutableState.update { it.copy(hasUnsavedProfileChanges = false) }
    }

    fun configurationReady(): Boolean = configurationReady(mutableState.value)

    fun resolvedVoice(selection: TtsDefaultVoiceSelection): TtsVoice? {
        val catalog = mutableState.value.voiceCatalog as? TtsVoiceCatalogState.Available ?: return null
        val voiceId = when (selection) {
            TtsDefaultVoiceSelection.EngineDefault -> catalog.defaultVoice
            is TtsDefaultVoiceSelection.Explicit -> selection.voice
        }
        return catalog.voices.singleOrNull { it.id == voiceId }
    }

    suspend fun openSetup(engine: TtsEngineId): TtsHostActionResult = hostActions.openSetup(engine)

    suspend fun installVoiceData(
        engine: TtsEngineId,
        languages: Set<LanguageTag>,
    ): TtsHostActionResult = hostActions.installVoiceData(engine, languages)

    suspend fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
    ): TtsHostActionResult = hostActions.acknowledgeProviderDisclosure(engine, disclosure)

    fun supportsSetup(engine: TtsEngineId): Boolean = hostActions.supportsSetup(engine)

    override fun close() {
        if (closed) return
        closed = true
        engineRefreshGeneration += 1
        voiceRefreshGeneration += 1
        engineRefreshJob?.cancel()
        voiceRefreshJob?.cancel()
        preferenceJobs.forEach(Job::cancel)
        previewPlayback.close()
    }

    private fun refreshVoices(engine: TtsEngineId?) {
        voiceRefreshJob?.cancel()
        val activeGeneration = ++voiceRefreshGeneration
        if (engine == null) {
            mutableState.update { it.copy(voiceCatalog = TtsVoiceCatalogState.NoEngine) }
            return
        }
        mutableState.update { it.copy(voiceCatalog = TtsVoiceCatalogState.Loading(engine)) }
        voiceRefreshJob = scope.launch {
            val catalog = when (val inspection = hostActions.inspectVoices(engine)) {
                is TtsVoiceInspection.Available -> TtsVoiceCatalogState.Available(
                    engine = engine,
                    voices = inspection.voices,
                    defaultVoice = inspection.defaultVoice,
                )
                is TtsVoiceInspection.VoiceDataRequired ->
                    TtsVoiceCatalogState.VoiceDataRequired(engine, inspection.reason)
                is TtsVoiceInspection.Unavailable -> TtsVoiceCatalogState.Unavailable(engine, inspection.reason)
                is TtsVoiceInspection.Failed -> TtsVoiceCatalogState.Failed(engine, inspection.reason)
            }
            if (closed || activeGeneration != voiceRefreshGeneration) return@launch
            val previewVoice = mutableState.value.previewVoice
            if (catalog !is TtsVoiceCatalogState.Available ||
                (previewVoice != null && catalog.voices.none { it.id == previewVoice })
            ) {
                stopPreview()
            }
            mutableState.update { current ->
                if (current.selectedEngine == engine) current.copy(voiceCatalog = catalog) else current
            }
        }
    }

    private fun voiceCatalogNeedsRefresh(engine: TtsEngineId?): Boolean {
        return when (val catalog = mutableState.value.voiceCatalog) {
            TtsVoiceCatalogState.NoEngine -> engine != null
            is TtsVoiceCatalogState.Loading -> catalog.engine != engine
            is TtsVoiceCatalogState.Available -> catalog.engine != engine
            is TtsVoiceCatalogState.VoiceDataRequired -> catalog.engine != engine
            is TtsVoiceCatalogState.Unavailable -> catalog.engine != engine
            is TtsVoiceCatalogState.Failed -> catalog.engine != engine
        }
    }

    fun stopPreview() {
        previewPlayback.stop()
        mutableState.update { it.copy(previewVoice = null) }
    }

    private fun toggleVoicePreview(voice: TtsVoice) {
        val state = mutableState.value
        val previewActive = state.preview == TtsPreviewState.Preparing || state.preview == TtsPreviewState.Speaking
        if (previewActive && state.previewVoice == voice.id) {
            stopPreview()
        } else {
            playVoicePreview(voice)
        }
    }

    private fun playVoicePreview(voice: TtsVoice) {
        val state = mutableState.value
        val engine = state.selectedEngine ?: return
        val speechRate = when (val support = state.selectedEngineState?.capabilities?.speechRate) {
            is TtsParameterSupport.Supported -> support.range.default
            TtsParameterSupport.Unsupported,
            null,
            -> 1f
        }
        val sample = previewSample(voice.language)
        mutableState.update { it.copy(previewVoice = voice.id) }
        previewPlayback.play(
            TtsRequest(
                text = sample.text,
                language = TtsLanguageSelection.Explicit(sample.language),
                engine = TtsEngineSelection.Explicit(engine),
                voice = mihon.tts.api.voice.TtsVoiceSelection.Explicit(voice.id),
                parameters = TtsParameterSelection.Explicit(
                    TtsParameters(speechRate = speechRate, pitch = state.pitch),
                ),
                processingPolicy = if (voice.processing == TtsVoiceProcessing.NetworkRequired) {
                    TtsProcessingPolicy.NetworkAllowed
                } else {
                    TtsProcessingPolicy.OnDeviceOnly
                },
            ),
        )
    }

    private fun configurationReady(state: TtsSettingsState): Boolean {
        val catalog = state.voiceCatalog as? TtsVoiceCatalogState.Available ?: return false
        val defaultVoice = resolvedVoice(state.defaultVoice) ?: return false
        if (catalog.engine != state.selectedEngine) return false
        if (state.voiceOverrides.any { (language, voice) ->
                catalog.voices.compatibleWith(language).none { it.id == voice }
            }
        ) {
            return false
        }
        if (!state.allowNetworkVoices) {
            val configuredVoiceIds = state.voiceOverrides.values + defaultVoice.id
            if (catalog.voices.any {
                    it.id in configuredVoiceIds && it.processing == TtsVoiceProcessing.NetworkRequired
                }
            ) {
                return false
            }
        }
        return when (val support = state.selectedEngineState?.capabilities?.pitch) {
            is TtsParameterSupport.Supported -> support.accepts(state.pitch)
            TtsParameterSupport.Unsupported -> state.pitch == 1f
            null -> false
        }
    }

    private fun TtsSettingsState.withUnsavedState(): TtsSettingsState {
        return copy(
            hasUnsavedProfileChanges = selectedEngine != savedEngine ||
                defaultVoice != savedDefaultVoice ||
                voiceOverrides != savedVoiceOverrides ||
                pitch != savedPitch ||
                allowNetworkVoices != savedAllowNetworkVoices,
        )
    }

    private fun TtsParameterSupport?.accepts(value: Float): Boolean {
        return when (this) {
            is TtsParameterSupport.Supported -> value in range.minimum..range.maximum
            TtsParameterSupport.Unsupported,
            null,
            -> false
        }
    }
}
