package mihon.translation.runtime

import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngine
import mihon.translation.spi.TranslationEngineRegistry

class DefaultTranslationEngineRegistry(
    engines: List<TranslationEngine>,
    knownEngines: List<KnownTranslationEngine> = engines.map(TranslationEngine::catalogEntry),
) : TranslationEngineRegistry, KnownTranslationEngineCatalog {
    override val engines: List<TranslationEngine> = engines.toList()
    override val knownEngines: List<KnownTranslationEngine> = knownEngines.toList()

    private val installedById: Map<TranslationEngineId, TranslationEngine>

    init {
        val duplicateInstalled = this.engines
            .groupingBy { it.catalogEntry.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateInstalled.isEmpty()) {
            "Duplicate installed Translation engines: ${duplicateInstalled.map { it.value }.sorted()}"
        }
        this.engines.forEach { engine ->
            val maximumInputCodePoints = engine.maximumInputCodePoints
            require(maximumInputCodePoints == null || maximumInputCodePoints > 0) {
                "Translation engine ${engine.catalogEntry.id.value} has an invalid input limit"
            }
        }

        val duplicateKnown = knownEngines
            .groupingBy(KnownTranslationEngine::id)
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateKnown.isEmpty()) {
            "Duplicate known Translation engines: ${duplicateKnown.map { it.value }.sorted()}"
        }

        val knownIds = knownEngines.mapTo(mutableSetOf(), KnownTranslationEngine::id)
        val missingCatalogEntries = this.engines.map { it.catalogEntry.id }.filterNot(knownIds::contains)
        require(missingCatalogEntries.isEmpty()) {
            "Installed Translation engines missing from the known catalog: " +
                missingCatalogEntries.map { it.value }.sorted()
        }

        installedById = this.engines.associateBy { it.catalogEntry.id }
    }

    override fun find(engine: TranslationEngineId): TranslationEngine? = installedById[engine]
}
