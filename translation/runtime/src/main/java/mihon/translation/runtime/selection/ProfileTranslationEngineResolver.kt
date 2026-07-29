package mihon.translation.runtime.selection

import kotlinx.coroutines.CancellationException
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.TranslationEngineDeviceAvailability
import mihon.translation.spi.TranslationEngineRegistry

internal class ProfileTranslationEngineResolver(
    private val preferences: ProfileTranslationPreferences,
    private val engineRegistry: TranslationEngineRegistry,
) {
    fun isExplicitlySelected(): Boolean = preferences.engine.isSet()

    fun resolve(engineStates: List<TranslationEngineState>): TranslationEngineId? {
        val selected = preferences.engine.get()
        if (isExplicitlySelected()) return selected
        return selected.takeIf { default ->
            engineStates.any { it.engine.id == default && it.status == TranslationEngineStatus.Ready }
        }
    }

    suspend fun resolve(): TranslationEngineId? {
        val selected = preferences.engine.get()
        if (isExplicitlySelected()) return selected
        val engine = engineRegistry.find(selected) ?: return null
        return try {
            selected.takeIf {
                engine.inspectDevice() == TranslationEngineDeviceAvailability.Available
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
}
