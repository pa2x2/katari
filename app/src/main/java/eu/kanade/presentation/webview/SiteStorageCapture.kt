package eu.kanade.presentation.webview

import android.webkit.WebView
import eu.kanade.tachiyomi.source.entry.SiteStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** Reads a page's web storage after each load and keeps the last reading, for a source that signs in through it. */
internal class SiteStorageCapture {
    var last: SiteStorage? = null
        private set

    fun read(view: WebView, url: String) {
        view.evaluateJavascript(READ_STORAGE) { result ->
            parse(url, result)?.let { last = it }
        }
    }

    // The result is the script's value as JSON: a string holding the JSON the script built, or null when the page
    // denies storage access before the script could run
    private fun parse(url: String, result: String?): SiteStorage? {
        val text = runCatching { (Json.parseToJsonElement(result ?: return null) as? JsonPrimitive)?.contentOrNull }
            .getOrNull() ?: return null
        val storage = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        return SiteStorage(url, storage.strings("local"), storage.strings("session"))
    }

    private fun JsonObject.strings(name: String): Map<String, String> =
        (get(name) as? JsonObject)?.mapNotNull { (key, value) ->
            (value as? JsonPrimitive)?.contentOrNull?.let { key to it }
        }?.toMap().orEmpty()
}

// A page may forbid one storage or both (sandboxed frames, disabled storage); that one reads as empty
private const val READ_STORAGE = """(function () {
  function read(name) {
    var values = {};
    try {
      var storage = window[name];
      for (var i = 0; i < storage.length; i++) {
        var key = storage.key(i);
        values[key] = storage.getItem(key);
      }
    } catch (e) {}
    return values;
  }
  return JSON.stringify({ local: read('localStorage'), session: read('sessionStorage') });
})()"""
