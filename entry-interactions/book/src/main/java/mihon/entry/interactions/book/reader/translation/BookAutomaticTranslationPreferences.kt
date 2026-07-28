package mihon.entry.interactions.book

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Profile-owned automatic translation preferences keyed by viewer settings surface.
 *
 * The former profile-wide value seeds each surface exactly once. Afterwards every surface is independent, including
 * when its value is deleted by a reader settings reset.
 */
internal class BookAutomaticTranslationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    private val legacyGlobalPreference = preferenceStore.getBoolean(
        key = LEGACY_GLOBAL_KEY,
        defaultValue = false,
    )
    private val migratedSurfaces = preferenceStore.getStringSet(
        key = MIGRATED_SURFACES_KEY,
        defaultValue = emptySet(),
    )
    private val preferencesBySurface = mutableMapOf<String, Preference<Boolean>>()

    fun automaticSelectionEnabled(settingsSurfaceId: String): Preference<Boolean> {
        require(SETTINGS_SURFACE_ID.matches(settingsSurfaceId)) {
            "Invalid viewer settings surface ID: '$settingsSurfaceId'"
        }
        return synchronized(preferencesBySurface) {
            preferencesBySurface.getOrPut(settingsSurfaceId) {
                val preference = preferenceStore.getBoolean(
                    key = "$SURFACE_KEY_PREFIX$settingsSurfaceId",
                    defaultValue = false,
                )
                LegacySeededPreference(
                    delegate = preference,
                    ensureMigrated = { ensureMigrated(settingsSurfaceId, preference) },
                )
            }
        }
    }

    private fun ensureMigrated(
        settingsSurfaceId: String,
        preference: Preference<Boolean>,
    ) {
        synchronized(migratedSurfaces) {
            val migrated = migratedSurfaces.get()
            if (settingsSurfaceId in migrated) return
            if (!preference.isSet() && legacyGlobalPreference.isSet()) {
                preference.set(legacyGlobalPreference.get())
            }
            migratedSurfaces.set(migrated + settingsSurfaceId)
        }
    }

    private class LegacySeededPreference(
        private val delegate: Preference<Boolean>,
        private val ensureMigrated: () -> Unit,
    ) : Preference<Boolean> {
        override fun key(): String = delegate.key()

        override fun get(): Boolean {
            ensureMigrated()
            return delegate.get()
        }

        override fun set(value: Boolean) {
            ensureMigrated()
            delegate.set(value)
        }

        override fun isSet(): Boolean {
            ensureMigrated()
            return delegate.isSet()
        }

        override fun delete() {
            ensureMigrated()
            delegate.delete()
        }

        override fun defaultValue(): Boolean = delegate.defaultValue()

        override fun changes(): Flow<Boolean> = delegate.changes()
            .map {
                ensureMigrated()
                delegate.get()
            }
            .distinctUntilChanged()

        override fun stateIn(scope: CoroutineScope): StateFlow<Boolean> =
            changes().stateIn(scope, SharingStarted.Eagerly, get())
    }

    companion object {
        const val LEGACY_GLOBAL_KEY = "translation_automatic_selection_enabled"
        const val SURFACE_KEY_PREFIX = "translation_automatic_selection_enabled:"
        const val MIGRATED_SURFACES_KEY = "translation_automatic_selection_migrated_surfaces"
        private val SETTINGS_SURFACE_ID = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}
