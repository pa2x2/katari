package mihon.tts.spi.contribution

import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.setup.TtsEngineSetup

/** One independently owned TTS engine contribution. */
data class TtsEngineContribution(
    val catalogEntry: KnownTtsEngine,
    val engine: TtsEngine? = null,
    val setup: TtsEngineSetup? = null,
    val order: Int = DEFAULT_ORDER,
) {
    init {
        require(engine == null || engine.catalogEntry == catalogEntry) {
            "TTS engine contribution catalog does not match its engine"
        }
        require(setup == null || setup.engine == catalogEntry.id) {
            "TTS engine contribution setup does not match its catalog"
        }
        require(engine != null || setup == null) {
            "Catalog-only TTS engine contributions cannot expose setup"
        }
    }

    constructor(
        engine: TtsEngine,
        setup: TtsEngineSetup? = null,
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
