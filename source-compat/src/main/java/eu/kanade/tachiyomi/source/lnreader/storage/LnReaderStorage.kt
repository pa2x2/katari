package eu.kanade.tachiyomi.source.lnreader.storage

import android.content.SharedPreferences
import mihon.script.runtime.ScriptBindings

/**
 * Where `@libs/storage` keeps a plugin's values: the plugin's source settings file, one key per storage key holding
 * LNReader's envelope as JSON. LNReader's settings screen writes plugin settings through this same storage, so the
 * file is also where Katari's settings screen reads and writes them.
 *
 * [preferences] is called on every access, in the runtime's profile, so it can resolve to that profile's file.
 */
class LnReaderStorage internal constructor(private val preferences: () -> SharedPreferences) {

    internal fun install(bindings: ScriptBindings) = with(bindings) {
        function("__lnreaderStorageGet") { (key) -> preferences().getString(key as String, null) }
        function("__lnreaderStorageSet") { (key, item) ->
            preferences().edit().putString(key as String, item as String).apply()
        }
        function("__lnreaderStorageDelete") { (key) -> preferences().edit().remove(key as String).apply() }
        function("__lnreaderStorageKeys") { preferences().all.keys.filter { it !in SITE_STORAGE_KEYS } }
        function("__lnreaderSiteStorage") { (kind) ->
            preferences().getString(if (kind == "local") LOCAL_STORAGE_KEY else SESSION_STORAGE_KEY, null)
        }
    }

    companion object {
        /**
         * The keys holding the site's own `localStorage` and `sessionStorage` as JSON objects, which plugins read
         * through `localStorage.get()` and `sessionStorage.get()`. They stay out of `getAllKeys` and `clearAll`.
         */
        const val LOCAL_STORAGE_KEY = "_LocalStorage"
        const val SESSION_STORAGE_KEY = "_SessionStorage"

        private val SITE_STORAGE_KEYS = setOf(LOCAL_STORAGE_KEY, SESSION_STORAGE_KEY)
    }
}
