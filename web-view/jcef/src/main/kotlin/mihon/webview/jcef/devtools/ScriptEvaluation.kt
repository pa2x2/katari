package mihon.webview.jcef.devtools

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Runs [script] in the page's main frame as the page's own scripts run, and returns its value as JSON; null where it
 * threw or the page couldn't run it. A value JSON has no form for, such as `undefined`, is `null`, and an object is the
 * JSON of its own properties, as a promise's `{}`.
 *
 * `Runtime.evaluate` needs no `Runtime.enable`, which a page's scripts could tell.
 */
internal suspend fun DevToolsSession.evaluate(script: String): String? {
    val response = call(
        "Runtime.evaluate",
        buildJsonObject {
            put("expression", script)
            put("returnByValue", true)
        },
    ) ?: return null
    if ("exceptionDetails" in response) return null
    val result = response["result"] as? JsonObject ?: return null
    return (result["value"] ?: JsonNull).toString()
}
