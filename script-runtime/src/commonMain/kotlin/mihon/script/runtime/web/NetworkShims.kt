package mihon.script.runtime.web

import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mihon.script.runtime.ScriptBindings
import mihon.script.runtime.ScriptNetwork
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * The host side of `fetch` (`web/fetch.js`). A request arrives as JSON with its header lines exactly as the script
 * built them; a response goes back as JSON metadata while its body stays here, read once the script asks for text or
 * bytes. Bodies live until the call that fetched them ends.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal class NetworkShims(private val network: ScriptNetwork) {
    private val bodies = ConcurrentHashMap<Int, ByteArray>()
    private val nextBodyId = AtomicInteger()

    fun install(bindings: ScriptBindings) = with(bindings) {
        asyncFunction("__hostFetch") { args -> fetch(args[0] as String) }
        function("__hostBody") { args ->
            val bytes = bodies[(args[0] as Number).toInt()] ?: throw IllegalStateException("The response body is gone")
            when (args[1]) {
                "text" -> bytes.decodeToString()
                else -> bytes.asUByteArray()
            }
        }
    }

    fun endCall() = bodies.clear()

    private suspend fun fetch(requestJson: String): String {
        val json = Json.parseToJsonElement(requestJson).jsonObject
        val call = network.client.newCall(json.toRequest())
        currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        call.await().use { response ->
            val (body, decoded) = response.decodedBody()
            val bodyId = nextBodyId.incrementAndGet()
            bodies[bodyId] = body.readByteArray()
            val headers = response.headers.let {
                if (decoded) it.newBuilder().removeAll("Content-Encoding").build() else it
            }
            return buildJsonObject {
                put("body", bodyId)
                put("status", response.code)
                put("statusText", response.message)
                put("url", response.request.url.toString())
                put("redirected", response.priorResponse != null)
                put(
                    "headers",
                    JsonArray(
                        headers.map { (name, value) ->
                            JsonArray(listOf(name, value).map(::JsonPrimitive))
                        },
                    ),
                )
            }.toString()
        }
    }

    private fun JsonObject.toRequest(): Request {
        val method = getValue("method").jsonPrimitive.content
        val headers = Headers.Builder().apply {
            getValue("headers").jsonArray.forEach { line ->
                val (name, value) = line.jsonArray.map { it.jsonPrimitive.content }
                addUnsafeNonAscii(name, value)
            }
            if (get("User-Agent") == null) add("User-Agent", network.userAgent())
        }.build()
        val body = (get("body") as? JsonObject)?.toRequestBody(headers["Content-Type"])
            ?: if (method in METHODS_WITH_BODY) ByteArray(0).toRequestBody() else null
        return Request.Builder()
            .url(getValue("url").jsonPrimitive.content)
            .headers(headers)
            .method(method, body)
            .build()
    }

    private fun JsonObject.toRequestBody(contentType: String?): RequestBody {
        val type = (contentType ?: get("type")?.jsonPrimitive?.contentOrNull)?.toMediaTypeOrNull()
        return when (getValue("kind").jsonPrimitive.content) {
            "form" -> MultipartBody.Builder().setType(MultipartBody.FORM).apply {
                getValue("parts").jsonArray.forEach { part ->
                    val (name, value) = part.jsonArray.map { it.jsonPrimitive.content }
                    addFormDataPart(name, value)
                }
            }.build()
            "bytes" -> Base64.getDecoder().decode(getValue("base64").jsonPrimitive.content).toRequestBody(type)
            // As bytes: OkHttp would otherwise add a charset to the script's Content-Type
            else -> getValue("text").jsonPrimitive.content.encodeToByteArray().toRequestBody(type)
        }
    }

    private companion object {
        val METHODS_WITH_BODY = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
    }
}
