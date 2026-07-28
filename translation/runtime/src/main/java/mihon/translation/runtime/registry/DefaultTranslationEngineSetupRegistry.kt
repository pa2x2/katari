package mihon.translation.runtime

import mihon.translation.api.TranslationEngineId
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationEngineSetupRegistry

class DefaultTranslationEngineSetupRegistry(
    setups: List<TranslationEngineSetup>,
) : TranslationEngineSetupRegistry {
    private val setupsByEngine = setups.associateBy(TranslationEngineSetup::engine)

    init {
        require(setupsByEngine.size == setups.size) {
            "Duplicate Translation engine setup adapters: " +
                setups.groupingBy(TranslationEngineSetup::engine)
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
                    .map { it.value }
                    .sorted()
        }
    }

    override fun find(engine: TranslationEngineId): TranslationEngineSetup? = setupsByEngine[engine]
}
