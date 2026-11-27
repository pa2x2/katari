package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue

/**
 * Aniyomi's hidden and pinned sources, incognito extensions and migration options. Each kind's sources and extensions
 * are joined into Katari's one set; anime sources take the converted backup's keys.
 */
internal fun AniyomiSettings.convertSourceSettings(animeSourceKey: (Long) -> Long) {
    ANIME_SOURCE_SETS.forEach { (mangaKey, animeKey) ->
        joinSets(mangaKey, animeKey) { id -> id.toLongOrNull()?.let(animeSourceKey)?.toString() }
    }
    joinSets("incognito_manga_extensions", "incognito_anime_extensions", mihonKey = "incognito_extensions") { it }
    take("migrate_flags")?.let { writeAsMihon(SourcePreferences.LEGACY_MIGRATION_FLAGS_KEY, it) }
}

/** Every anime source the settings name, which the converted backup has to give a key. */
internal fun animeSourceIdsInSettings(backedUp: List<BackupPreference>): Set<Long> = backedUp
    .filter { it.key in ANIME_SOURCE_SETS.map { (_, animeKey) -> animeKey } }
    .flatMap { (it.value as? StringSetPreferenceValue)?.value.orEmpty() }
    .mapNotNullTo(hashSetOf()) { it.toLongOrNull() }

/** Mihon's older keys for hidden and pinned sources, which Mihon's conversion renames, and Aniyomi's anime keys. */
private val ANIME_SOURCE_SETS = listOf(
    SourcePreferences.LEGACY_HIDDEN_SOURCES_KEY to "hidden_anime_catalogues",
    SourcePreferences.MANGA_PINNED_SOURCES_KEY to "pinned_anime_catalogues",
)
