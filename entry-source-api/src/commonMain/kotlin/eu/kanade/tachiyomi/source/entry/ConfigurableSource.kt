package eu.kanade.tachiyomi.source.entry

import android.content.SharedPreferences

/**
 * A [UnifiedSource] that exposes a preference screen.
 *
 * This replaces the separate manga and anime configurable source interfaces.
 * The legacy manga adapter bridges [eu.kanade.tachiyomi.source.ConfigurableSource]
 * so existing extension preferences keep working under their existing keys.
 */
interface ConfigurableSource : UnifiedSource {

    /**
     * Gets an instance of [SharedPreferences] scoped to the specific source.
     */
    fun getSourcePreferences(): SharedPreferences =
        getEntryPreferenceProvider().preferences(id)

    fun setupPreferenceScreen(screen: EntryPreferenceScreen)
}

fun ConfigurableSource.preferenceKey(): String = getEntryPreferenceProvider().key(id)
