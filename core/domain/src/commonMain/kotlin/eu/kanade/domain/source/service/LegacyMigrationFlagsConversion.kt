package eu.kanade.domain.source.service

import tachiyomi.core.common.preference.PreferenceStore

/**
 * Keeps the download choice of the remembered migration options, which also saved what to carry over. What a
 * migration carries over now starts from everything each time, so only the download bit survives. A store with the old
 * options replaces the choice it holds, so a restored backup's choice wins.
 */
object LegacyMigrationFlagsConversion {

    /** The old options' bit for deleting the old entry's downloads. */
    private const val REMOVE_DOWNLOADS_BIT = 0b10000

    fun convert(store: PreferenceStore) {
        val legacy = store.getInt(SourcePreferences.LEGACY_MIGRATION_FLAGS_KEY, 0)
        if (!legacy.isSet()) return

        SourcePreferences.migrationRemoveDownloads(store).set(legacy.get() and REMOVE_DOWNLOADS_BIT != 0)
        legacy.delete()
    }
}
