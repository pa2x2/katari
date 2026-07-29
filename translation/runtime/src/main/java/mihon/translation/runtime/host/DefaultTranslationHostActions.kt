package mihon.translation.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineAction
import mihon.translation.api.TranslationEngineBuildAvailability
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineInspection
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageSupportInspection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.runtime.selection.ProfileTranslationEngineResolver
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineRegistry
import mihon.translation.spi.TranslationEngineSetupRegistry
import mihon.translation.spi.TranslationSetupResult
import tachiyomi.core.common.preference.Preference
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultTranslationHostActions(
    private val preferences: ProfileTranslationPreferences,
    private val engineRegistry: TranslationEngineRegistry,
    knownEngineCatalog: KnownTranslationEngineCatalog,
    private val setupRegistry: TranslationEngineSetupRegistry,
    private val profileEngineResolver: ProfileTranslationEngineResolver,
    inspectionDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val inspectionTimeoutMillis: Long = DEFAULT_ENGINE_INSPECTION_TIMEOUT_MILLIS,
) : TranslationHostActions {
    private val inspectionScope = CoroutineScope(SupervisorJob() + inspectionDispatcher)
    override val knownEngines: List<KnownTranslationEngine> = knownEngineCatalog.knownEngines
    override val selectedEngine: Preference<TranslationEngineId> = preferences.engine
    override val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection> = preferences.targetLanguage

    init {
        require(inspectionTimeoutMillis > 0)
    }

    override suspend fun deviceAvailability(): TranslationDeviceAvailability {
        if (!profileEngineResolver.isExplicitlySelected()) {
            return if (profileEngineResolver.resolve() != null) {
                TranslationDeviceAvailability.Available
            } else {
                TranslationDeviceAvailability.EngineNotConfigured
            }
        }
        val selected = selectedEngine.get()
        val engine = engineRegistry.find(selected)
            ?: return if (knownEngines.any { it.id == selected }) {
                TranslationDeviceAvailability.SelectedEngineUnavailable(selected)
            } else {
                TranslationDeviceAvailability.SelectedEngineMissing(selected)
            }
        return try {
            when (val availability = engine.inspectDevice()) {
                TranslationEngineDeviceAvailability.Available -> TranslationDeviceAvailability.Available
                TranslationEngineDeviceAvailability.NotInstalled ->
                    TranslationDeviceAvailability.SelectedEngineUnavailable(
                        selected,
                        "Provider application is not installed",
                    )
                is TranslationEngineDeviceAvailability.ConfigurationRequired ->
                    TranslationDeviceAvailability.SelectedEngineUnavailable(selected, availability.reason)
                is TranslationEngineDeviceAvailability.UnsupportedOs ->
                    TranslationDeviceAvailability.UnsupportedOs(availability.minimumApi)
                TranslationEngineDeviceAvailability.ServiceMissing ->
                    TranslationDeviceAvailability.TranslationServiceMissing
                is TranslationEngineDeviceAvailability.Unavailable ->
                    TranslationDeviceAvailability.SelectedEngineUnavailable(selected, availability.reason)
                is TranslationEngineDeviceAvailability.Failed ->
                    TranslationDeviceAvailability.ProviderFailure(selected, availability.reason)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationDeviceAvailability.ProviderFailure(
                selected,
                "Translation engine availability could not be inspected",
            )
        }
    }

    override suspend fun inspectEngines(): TranslationEngineInspection = inspectEngineStates().last()

    override fun inspectEngineStates(): Flow<TranslationEngineInspection> = flow {
        var states = knownEngines.map(::initialEngineState)
        emit(states.toInspection())

        val pending = states.mapIndexedNotNull { index, state ->
            engineRegistry.find(state.engine.id)
                ?.takeIf { state.status == TranslationEngineStatus.Checking }
                ?.let { PendingEngineInspection(index, it) }
        }
        if (pending.isEmpty()) return@flow

        val updates = Channel<IndexedValue<TranslationEngineState>>(capacity = pending.size)
        val jobs = pending.flatMap { pendingInspection ->
            val accepted = AtomicBoolean()
            val providerJob = inspectionScope.launch {
                val inspected = inspectedEngineState(pendingInspection.engine)
                if (accepted.compareAndSet(false, true)) {
                    updates.trySend(IndexedValue(pendingInspection.index, inspected))
                }
            }
            val timeoutJob = inspectionScope.launch {
                delay(inspectionTimeoutMillis)
                if (accepted.compareAndSet(false, true)) {
                    providerJob.cancel()
                    updates.trySend(
                        IndexedValue(
                            pendingInspection.index,
                            timedOutEngineState(pendingInspection.engine),
                        ),
                    )
                }
            }
            listOf(providerJob, timeoutJob)
        }

        try {
            repeat(pending.size) {
                val update = updates.receive()
                states = states.toMutableList().apply {
                    this[update.index] = update.value
                }
                emit(states.toInspection())
            }
        } finally {
            jobs.forEach(Job::cancel)
            updates.cancel()
        }
    }

    override suspend fun inspectLanguageSupport(
        engine: TranslationEngineId,
    ): TranslationLanguageSupportInspection {
        val installed = engineRegistry.find(engine)
            ?: return TranslationLanguageSupportInspection.Unavailable(
                "Translation engine is not available",
            )
        return try {
            installed.inspectLanguageSupport()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationLanguageSupportInspection.Unavailable(
                "Translation languages could not be inspected",
            )
        }
    }

    override suspend fun acknowledgeProviderDisclosure(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
    ): TranslationHostActionResult = performSetup(engine) {
        acknowledge(disclosure)
        TranslationHostActionResult.Completed
    }

    override suspend fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        allowMeteredNetwork: Boolean,
    ): TranslationHostActionResult = performSetup(engine) {
        when (
            val result = downloadModels(
                models = models.mapTo(mutableSetOf()) { it.id },
                allowMeteredNetwork = allowMeteredNetwork,
            )
        ) {
            TranslationModelOperationResult.Completed -> TranslationHostActionResult.ModelsReady
            is TranslationModelOperationResult.Failed -> TranslationHostActionResult.ModelsFailed(result.reason)
        }
    }

    override fun supportsSetup(engine: TranslationEngineId): Boolean =
        setupRegistry.findSetup(engine)?.supportsSetup == true

    override suspend fun openSetup(engine: TranslationEngineId): TranslationHostActionResult =
        performSetup(engine) {
            when (val result = openSetup()) {
                is TranslationSetupResult.Opened ->
                    TranslationHostActionResult.SetupOpened(result.destination)
                TranslationSetupResult.Unsupported -> TranslationHostActionResult.SetupUnsupported
                TranslationSetupResult.ServiceMissing -> TranslationHostActionResult.ServiceMissing
                TranslationSetupResult.SettingsUnavailable -> TranslationHostActionResult.SettingsUnavailable
                is TranslationSetupResult.Failed -> TranslationHostActionResult.Failed(result.reason)
            }
        }

    override fun setSelectedEngine(engine: TranslationEngineId) {
        selectedEngine.set(engine)
    }

    override fun setDefaultTargetLanguage(language: TranslationLanguageTag?) {
        defaultTargetLanguage.set(
            language?.let(TranslationTargetLanguageSelection::Explicit)
                ?: TranslationTargetLanguageSelection.Default,
        )
    }

    private suspend fun performSetup(
        engine: TranslationEngineId,
        action: suspend mihon.translation.spi.TranslationEngineSetup.() -> TranslationHostActionResult,
    ): TranslationHostActionResult {
        val setup = setupRegistry.findSetup(engine) ?: return TranslationHostActionResult.SetupUnsupported
        return try {
            setup.action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationHostActionResult.Failed("Unexpected translation setup failure")
        }
    }

    private suspend fun inspectEngineState(
        engine: mihon.translation.spi.TranslationEngine,
    ): TranslationEngineStatus {
        return try {
            when (val availability = engine.inspectDevice()) {
                TranslationEngineDeviceAvailability.Available -> TranslationEngineStatus.Ready
                TranslationEngineDeviceAvailability.NotInstalled -> TranslationEngineStatus.NotInstalled
                is TranslationEngineDeviceAvailability.ConfigurationRequired ->
                    TranslationEngineStatus.ConfigurationRequired(availability.reason)
                is TranslationEngineDeviceAvailability.UnsupportedOs ->
                    TranslationEngineStatus.Unavailable(
                        TranslationUnavailableReason.UnsupportedOs(availability.minimumApi),
                    )
                TranslationEngineDeviceAvailability.ServiceMissing ->
                    TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing)
                is TranslationEngineDeviceAvailability.Unavailable ->
                    TranslationEngineStatus.Unavailable(
                        TranslationUnavailableReason.EngineUnavailable(
                            engine.catalogEntry.id,
                            availability.reason ?: "Provider is unavailable",
                        ),
                    )
                is TranslationEngineDeviceAvailability.Failed ->
                    TranslationEngineStatus.Unavailable(
                        TranslationUnavailableReason.EngineUnavailable(
                            engine.catalogEntry.id,
                            availability.reason,
                        ),
                    )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TranslationEngineStatus.Unavailable(
                TranslationUnavailableReason.EngineUnavailable(
                    engine.catalogEntry.id,
                    "Engine availability could not be inspected",
                ),
            )
        }
    }

    private fun initialEngineState(known: KnownTranslationEngine): TranslationEngineState {
        val engine = engineRegistry.find(known.id)
        val status = when (val buildAvailability = known.buildAvailability) {
            is TranslationEngineBuildAvailability.NotIncluded ->
                TranslationEngineStatus.Unavailable(
                    TranslationUnavailableReason.EngineUnavailable(
                        known.id,
                        buildAvailability.reason,
                    ),
                )
            TranslationEngineBuildAvailability.Included -> if (engine == null) {
                TranslationEngineStatus.Unavailable(
                    TranslationUnavailableReason.EngineUnavailable(
                        known.id,
                        "Engine is not included in this build",
                    ),
                )
            } else {
                TranslationEngineStatus.Checking
            }
        }
        return engineState(known, engine, status)
    }

    private suspend fun inspectedEngineState(
        engine: mihon.translation.spi.TranslationEngine,
    ): TranslationEngineState = engineState(
        known = engine.catalogEntry,
        engine = engine,
        status = inspectEngineState(engine),
    )

    private fun timedOutEngineState(
        engine: mihon.translation.spi.TranslationEngine,
    ): TranslationEngineState = engineState(
        known = engine.catalogEntry,
        engine = engine,
        status = TranslationEngineStatus.Unavailable(
            TranslationUnavailableReason.EngineUnavailable(
                engine.catalogEntry.id,
                "Engine availability check timed out",
            ),
        ),
    )

    private fun engineState(
        known: KnownTranslationEngine,
        engine: mihon.translation.spi.TranslationEngine?,
        status: TranslationEngineStatus,
    ): TranslationEngineState {
        val supportsSetup = setupRegistry.findSetup(known.id)?.supportsSetup == true
        return TranslationEngineState(
            engine = known,
            presentation = engine?.presentation,
            status = status,
            action = when {
                !supportsSetup || status == TranslationEngineStatus.Checking -> null
                status is TranslationEngineStatus.NotInstalled -> TranslationEngineAction.Install
                status is TranslationEngineStatus.ConfigurationRequired -> TranslationEngineAction.Configure
                else -> TranslationEngineAction.Setup
            },
        )
    }

    private fun List<TranslationEngineState>.toInspection(): TranslationEngineInspection {
        val preferredState = firstOrNull { it.engine.id == selectedEngine.get() }
        val selectionResolved = profileEngineResolver.isExplicitlySelected() ||
            preferredState?.status != TranslationEngineStatus.Checking
        return TranslationEngineInspection(
            engines = this,
            selectedEngine = takeIf { selectionResolved }
                ?.let(profileEngineResolver::resolve),
            selectionResolved = selectionResolved,
        )
    }

    private data class PendingEngineInspection(
        val index: Int,
        val engine: mihon.translation.spi.TranslationEngine,
    )

    private companion object {
        const val DEFAULT_ENGINE_INSPECTION_TIMEOUT_MILLIS = 10_000L
    }
}
