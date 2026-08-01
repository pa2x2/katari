package mihon.entry.interactions.reader.preparation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ReaderChapterPreparationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    private val legacyPreference = preferenceStore.getBoolean(LEGACY_PREPARE_NEXT_CHAPTER_KEY, false)
    private val migratedSurfaces = preferenceStore.getStringSet(MIGRATED_SURFACES_KEY, emptySet())
    private val preferencesBySurface = mutableMapOf<String, Preference<Boolean>>()

    @Volatile
    private var legacyMigrationSurfaceIds = emptySet<String>()

    fun prepareNextChapter(settingsSurfaceId: String): Preference<Boolean> {
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

    fun completeLegacyMigration(settingsSurfaceIds: Set<String>) {
        if (settingsSurfaceIds.isEmpty()) return
        legacyMigrationSurfaceIds = settingsSurfaceIds
        settingsSurfaceIds.forEach { settingsSurfaceId ->
            prepareNextChapter(settingsSurfaceId).get()
        }
    }

    private fun ensureMigrated(
        settingsSurfaceId: String,
        preference: Preference<Boolean>,
    ) {
        synchronized(migratedSurfaces) {
            val migrated = migratedSurfaces.get()
            if (settingsSurfaceId in migrated) {
                deleteLegacyPreferenceIfComplete(migrated)
                return
            }
            if (!preference.isSet() && legacyPreference.isSet()) {
                preference.set(legacyPreference.get())
            }
            val updated = migrated + settingsSurfaceId
            migratedSurfaces.set(updated)
            deleteLegacyPreferenceIfComplete(updated)
        }
    }

    private fun deleteLegacyPreferenceIfComplete(migratedSurfaces: Set<String>) {
        val requiredSurfaces = legacyMigrationSurfaceIds
        if (requiredSurfaces.isNotEmpty() && requiredSurfaces.all { it in migratedSurfaces }) {
            legacyPreference.delete()
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
        const val LEGACY_PREPARE_NEXT_CHAPTER_KEY = "reader_prepare_next_chapter"
        const val SURFACE_KEY_PREFIX = "reader_prepare_next_chapter:"
        const val MIGRATED_SURFACES_KEY = "reader_prepare_next_chapter_migrated_surfaces"
        private val SETTINGS_SURFACE_ID = Regex("""[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*""")
    }
}
