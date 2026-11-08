package eu.kanade.tachiyomi.source.entry

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Unified preference provider for all content sources.
 *
 * Legacy manga extension preferences keep working under their existing `source_$id`
 * keys. Anime sources use the same keys now that source APIs are unified.
 */
interface EntryPreferenceProvider {
    /** Returns the stable preference-file key for [sourceId]. */
    fun key(sourceId: Long): String

    /** Returns preferences scoped to [sourceId]. */
    fun preferences(sourceId: Long): SharedPreferences
}

/** Default Android-backed source preference provider. */
object DefaultEntryPreferenceProvider : EntryPreferenceProvider {
    /** Returns the legacy-compatible `source_<id>` key. */
    override fun key(sourceId: Long): String = "source_$sourceId"

    /** Opens the private Android preference file for [sourceId]. */
    override fun preferences(sourceId: Long): SharedPreferences {
        return Injekt.get<Application>().getSharedPreferences(key(sourceId), Context.MODE_PRIVATE)
    }
}

/** Returns the injected preference provider or the default Android implementation. */
fun getEntryPreferenceProvider(): EntryPreferenceProvider {
    return runCatching { Injekt.get<EntryPreferenceProvider>() }
        .getOrElse { DefaultEntryPreferenceProvider }
}
