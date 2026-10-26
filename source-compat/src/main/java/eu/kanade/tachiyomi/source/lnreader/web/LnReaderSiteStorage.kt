package eu.kanade.tachiyomi.source.lnreader.web

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.SiteStorage
import eu.kanade.tachiyomi.source.lnreader.runtime.storage.LnReaderStorage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Keeps the site's own storage, as the WebView last read it, where the plugin reads it: LNReader's reserved keys in
 * the plugin's settings file, as LNReader's WebView saves them when the user leaves it.
 */
internal fun SharedPreferences.saveSiteStorage(storage: SiteStorage) {
    edit()
        .putString(LnReaderStorage.LOCAL_STORAGE_KEY, storage.local.toJson())
        .putString(LnReaderStorage.SESSION_STORAGE_KEY, storage.session.toJson())
        .apply()
}

private fun Map<String, String>.toJson(): String = JsonObject(mapValues { JsonPrimitive(it.value) }).toString()
