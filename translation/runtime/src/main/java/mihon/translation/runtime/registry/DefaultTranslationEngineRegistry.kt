package mihon.translation.runtime.registry

import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.spi.contribution.TranslationEngineContribution
import mihon.translation.spi.engine.KnownTranslationEngineCatalog
import mihon.translation.spi.engine.TranslationEngine
import mihon.translation.spi.engine.TranslationEngineRegistry
import mihon.translation.spi.setup.TranslationEngineSetup
import mihon.translation.spi.setup.TranslationEngineSetupRegistry

class DefaultTranslationEngineRegistry(
    contributions: List<TranslationEngineContribution>,
) : TranslationEngineRegistry, KnownTranslationEngineCatalog, TranslationEngineSetupRegistry {
    val contributions: List<TranslationEngineContribution> = contributions
        .sortedWith(compareBy(TranslationEngineContribution::order, { it.catalogEntry.id.value }))
    override val engines: List<TranslationEngine> = this.contributions.mapNotNull { it.engine }
    override val knownEngines: List<KnownTranslationEngine> = this.contributions.map { it.catalogEntry }

    private val installedById: Map<TranslationEngineId, TranslationEngine>
    private val setupsByEngine: Map<TranslationEngineId, TranslationEngineSetup>

    init {
        val duplicateContributions = this.contributions
            .groupingBy { it.catalogEntry.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateContributions.isEmpty()) {
            "Duplicate Translation engine contributions: ${duplicateContributions.map { it.value }.sorted()}"
        }
        this.engines.forEach { engine ->
            val maximumInputCodePoints = engine.maximumInputCodePoints
            require(maximumInputCodePoints == null || maximumInputCodePoints > 0) {
                "Translation engine ${engine.catalogEntry.id.value} has an invalid input limit"
            }
        }

        installedById = this.engines.associateBy { it.catalogEntry.id }
        setupsByEngine = this.contributions.mapNotNull { contribution ->
            contribution.setup?.let { contribution.catalogEntry.id to it }
        }.toMap()
    }

    override fun find(engine: TranslationEngineId): TranslationEngine? = installedById[engine]

    override fun findSetup(engine: TranslationEngineId): TranslationEngineSetup? = setupsByEngine[engine]
}
