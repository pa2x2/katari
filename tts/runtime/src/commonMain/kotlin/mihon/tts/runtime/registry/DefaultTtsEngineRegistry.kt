package mihon.tts.runtime.registry

import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.spi.contribution.TtsEngineContribution
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineRegistry
import mihon.tts.spi.setup.TtsEngineSetup
import mihon.tts.spi.setup.TtsEngineSetupRegistry

class DefaultTtsEngineRegistry(
    contributions: List<TtsEngineContribution>,
) : TtsEngineRegistry, KnownTtsEngineCatalog, TtsEngineSetupRegistry {
    val contributions: List<TtsEngineContribution> = contributions
        .sortedWith(compareBy(TtsEngineContribution::order, { it.catalogEntry.id.value }))
    override val engines: List<TtsEngine> = this.contributions.mapNotNull(TtsEngineContribution::engine)
    override val knownEngines: List<KnownTtsEngine> = this.contributions.map(TtsEngineContribution::catalogEntry)

    private val enginesById: Map<TtsEngineId, TtsEngine>
    private val setupsById: Map<TtsEngineId, TtsEngineSetup>

    init {
        val duplicateIds = this.contributions
            .groupingBy { it.catalogEntry.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateIds.isEmpty()) {
            "Duplicate TTS engine contributions: ${duplicateIds.map { it.value }.sorted()}"
        }
        enginesById = engines.associateBy { it.catalogEntry.id }
        setupsById = this.contributions.mapNotNull { contribution ->
            contribution.setup?.let { contribution.catalogEntry.id to it }
        }.toMap()
    }

    override fun find(engine: TtsEngineId): TtsEngine? = enginesById[engine]

    override fun findSetup(engine: TtsEngineId): TtsEngineSetup? = setupsById[engine]
}
