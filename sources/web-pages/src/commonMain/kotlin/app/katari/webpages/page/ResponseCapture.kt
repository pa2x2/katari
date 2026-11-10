package app.katari.webpages.page

import eu.kanade.tachiyomi.source.entry.webpage.WebPageResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom

/**
 * How a page's responses reach the app: [script], run before the document's own scripts, wraps `fetch` and
 * `XMLHttpRequest` and posts each response to the [channel], which [read] turns back into a response.
 *
 * Neither platform's web view hands the app a response's body, and a wrapper added any later misses the requests a
 * document makes as it starts.
 *
 * Only the top document is wrapped, so a frame of another site can't make requests that pass for the page's. Posts
 * carry a secret only the wrapper's closure holds, as any frame can call the channel.
 */
internal class ResponseCapture {
    val channel: String = "_" + randomLetters(CHANNEL_LETTERS)
    private val secret: String = randomLetters(SECRET_LETTERS)

    val script: String = """
        (() => {
            if (window !== window.top) return;
            const secret = ${JsonPrimitive(secret)};
            const channel = ${JsonPrimitive(channel)};
            const stringify = JSON.stringify;
            const post = (entry) => {
                try {
                    window[channel].postMessage(stringify([secret, entry]));
                } catch (e) {
                    // A response that can't be told is one the source's wait runs out for
                }
            };
            const textual = (type) => !type
                || /^text\/|^application\/([\w.+-]*\+)?(json|xml)|^application\/(javascript|x-www-form-urlencoded)|mpegurl/i
                    .test(type);
            const absolute = (url) => {
                try {
                    return new URL(url, location.href).href;
                } catch (e) {
                    return String(url);
                }
            };

            const fetchPage = window.fetch;
            if (fetchPage) {
                window.fetch = function (input, init) {
                    const request = input instanceof Request ? input : null;
                    const url = absolute(request ? request.url : input);
                    const method = ((init && init.method) || (request && request.method) || 'GET').toUpperCase();
                    const sent = init && typeof init.body === 'string' ? init.body : null;
                    const answer = fetchPage.apply(this, arguments);
                    answer.then((response) => {
                        const entry = { url, method, status: response.status, sent, body: '' };
                        if (!textual(response.headers.get('content-type'))) return post(entry);
                        response.clone().text().then((body) => post({ ...entry, body }), () => post(entry));
                    }, () => {});
                    return answer;
                };
            }

            const open = XMLHttpRequest.prototype.open;
            const send = XMLHttpRequest.prototype.send;
            const requests = new WeakMap();
            XMLHttpRequest.prototype.open = function (method, url) {
                requests.set(this, { method: String(method).toUpperCase(), url: absolute(url) });
                return open.apply(this, arguments);
            };
            XMLHttpRequest.prototype.send = function (body) {
                const request = requests.get(this);
                if (request) {
                    this.addEventListener('load', () => {
                        let text = '';
                        if (this.responseType === '' || this.responseType === 'text') {
                            text = this.responseText;
                        } else if (this.responseType === 'json') {
                            text = stringify(this.response);
                        }
                        post({
                            url: request.url,
                            method: request.method,
                            status: this.status,
                            sent: typeof body === 'string' ? body : null,
                            body: text,
                        });
                    });
                }
                return send.apply(this, arguments);
            };
        })();
    """.trimIndent()

    /** The response [message] tells of; null for a post that isn't the wrapper's. */
    fun read(message: String): WebPageResponse? {
        val post = runCatching { Json.parseToJsonElement(message).jsonArray }.getOrNull() ?: return null
        if (post.getOrNull(0)?.let { it as? JsonPrimitive }?.contentOrNull != secret) return null
        val entry = post.getOrNull(1) as? JsonObject ?: return null
        return WebPageResponse(
            url = entry.text("url") ?: return null,
            method = entry.text("method") ?: return null,
            status = (entry["status"] as? JsonPrimitive)?.intOrNull ?: return null,
            requestBody = entry.text("sent"),
            body = entry.text("body").orEmpty(),
        )
    }

    private fun JsonObject.text(name: String): String? =
        get(name)?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

    private companion object {
        const val CHANNEL_LETTERS = 12
        const val SECRET_LETTERS = 24
        const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"

        fun randomLetters(count: Int): String {
            val random = SecureRandom()
            return buildString { repeat(count) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
        }
    }
}
