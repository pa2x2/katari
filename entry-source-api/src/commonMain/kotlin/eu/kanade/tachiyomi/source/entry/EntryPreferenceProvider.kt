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
    fun key(sourceId: Long): String
    fun preferences(sourceId: Long): SharedPreferences
}

object DefaultEntryPreferenceProvider : EntryPreferenceProvider {
    override fun key(sourceId: Long): String = "source_$sourceId"

    override fun preferences(sourceId: Long): SharedPreferences {
        return Injekt.get<Application>().getSharedPreferences(key(sourceId), Context.MODE_PRIVATE)
    }
}

fun getEntryPreferenceProvider(): EntryPreferenceProvider {
    return runCatching { Injekt.get<EntryPreferenceProvider>() }
        .getOrElse { DefaultEntryPreferenceProvider }
}
