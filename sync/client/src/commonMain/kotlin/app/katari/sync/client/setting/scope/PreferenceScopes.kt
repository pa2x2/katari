package app.katari.sync.client.setting.scope

import tachiyomi.core.common.preference.Preference

/** The scope of every preference key, from the tables of every area. */
object PreferenceScopes {
    private val tables = listOf(
        AppearanceScopes,
        LibraryScopes,
        BrowseScopes,
        StatisticsScopes,
        DownloadScopes,
        TrackingScopes,
        MangaReaderScopes,
        BookReaderScopes,
        PlayerScopes,
        EntryPreviewScopes,
        TranslationScopes,
        SpeechScopes,
        TextRecognitionScopes,
        SecurityScopes,
        SystemScopes,
    )

    private val keys = merge { it.keys }
    private val prefixes = merge { it.prefixes }.entries.sortedByDescending { it.key.length }

    /**
     * [key] as preference classes ask a store for it. A key in a family takes the scope of the longest prefix it
     * starts with; a key no table marks stays on the device.
     */
    fun of(key: String): PreferenceScope {
        if (Preference.isAppState(key) || Preference.isPrivate(key)) return PreferenceScope.DEVICE
        keys[key]?.let { return it }
        return prefixes.firstOrNull { key.startsWith(it.key) }?.value ?: PreferenceScope.DEVICE
    }

    private fun merge(of: (PreferenceScopeTable) -> Map<String, PreferenceScope>): Map<String, PreferenceScope> {
        val merged = HashMap<String, PreferenceScope>()
        tables.forEach { table ->
            of(table).forEach { (key, scope) -> require(merged.put(key, scope) == null) { "$key is marked twice" } }
        }
        return merged
    }
}
