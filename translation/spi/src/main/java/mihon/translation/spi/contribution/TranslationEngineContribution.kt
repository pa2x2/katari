package mihon.translation.spi

import mihon.translation.api.KnownTranslationEngine

/**
 * One independently owned Translation engine contribution.
 *
 * A contribution may contain only a catalog entry when an engine is intentionally
 * excluded from the current build. Runtime-capable contributions keep execution
 * and setup ownership together so shared composition never has to correlate
 * parallel provider lists.
 */
data class TranslationEngineContribution(
    val catalogEntry: KnownTranslationEngine,
    val engine: TranslationEngine? = null,
    val setup: TranslationEngineSetup? = null,
    val order: Int = DEFAULT_ORDER,
) {
    init {
        require(engine == null || engine.catalogEntry == catalogEntry) {
            "Translation engine contribution catalog does not match its engine"
        }
        require(setup == null || setup.engine == catalogEntry.id) {
            "Translation engine contribution setup does not match its catalog"
        }
        require(engine != null || setup == null) {
            "Catalog-only Translation engine contributions cannot expose setup"
        }
    }

    constructor(
        engine: TranslationEngine,
        setup: TranslationEngineSetup? = null,
        order: Int = DEFAULT_ORDER,
    ) : this(
        catalogEntry = engine.catalogEntry,
        engine = engine,
        setup = setup,
        order = order,
    )

    private companion object {
        const val DEFAULT_ORDER = 0
    }
}
