package app.katari.sync.client.setting.scope

import tachiyomi.core.common.preference.Preference

/** The mark of every preference key, from the tables of every part of the app. */
object PreferenceScopes {
    private val tables = listOf(
        AppearanceScopes,
        LibraryScopes,
        BrowseScopes,
        StatisticsScopes,
        DownloadScopes,
        MangaReaderScopes,
        BookReaderScopes,
        PlayerScopes,
        EntryPreviewScopes,
        TranslationScopes,
        SpeechScopes,
        TextRecognitionScopes,
        SecurityScopes,
        SystemScopes,
        SyncScopes,
    )

    private val keys = merge { it.keys }
    private val prefixes = merge { it.prefixes }.entries.sortedByDescending { it.key.length }

    fun of(key: String): PreferenceScope = markOf(key)?.scope ?: PreferenceScope.DEVICE

    /**
     * The mark of [key] as preference classes ask a store for it. A key in a family takes the mark of the longest
     * prefix it starts with. Null for an app-state or private key, and for a key no table marks, which stays on the
     * device.
     */
    fun markOf(key: String): PreferenceMark? {
        if (Preference.isAppState(key) || Preference.isPrivate(key)) return null
        keys[key]?.let { return it }
        return prefixes.firstOrNull { key.startsWith(it.key) }?.value
    }

    private fun merge(of: (PreferenceScopeTable) -> Map<String, PreferenceMark>): Map<String, PreferenceMark> {
        val merged = HashMap<String, PreferenceMark>()
        tables.forEach { table ->
            of(table).forEach { (key, mark) -> require(merged.put(key, mark) == null) { "$key is marked twice" } }
        }
        return merged
    }
}
