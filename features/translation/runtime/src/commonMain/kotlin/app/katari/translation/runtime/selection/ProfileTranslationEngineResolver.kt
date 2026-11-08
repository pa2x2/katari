package app.katari.translation.runtime.selection

import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineState
import app.katari.translation.api.engine.TranslationEngineStatus
import app.katari.translation.runtime.preference.ProfileTranslationPreferences
import app.katari.translation.spi.engine.TranslationEngineDeviceAvailability
import app.katari.translation.spi.engine.TranslationEngineRegistry
import kotlinx.coroutines.CancellationException

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
