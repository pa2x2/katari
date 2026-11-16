package app.katari.webview.engine.opener

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.security.SecureRandom
import java.util.Base64

/**
 * The link between a page and the popups it opens, which Chromium keeps only for popups it opens itself and the app
 * opens each popup as a page of its own. A script added at the start of every document of the page's main frame,
 * before the page's own scripts, stands in for the link: `window.open` returns a window whose `postMessage`, `close`
 * and `closed` go through the app to the popup's page, and in a popup, `window.opener` does the same for the page that
 * opened it and `window.close` closes the popup. Sign-in popups, such as Google's, hand their result back that way.
 *
 * The script tells the app what to carry by logging it to the console behind a secret of this page's, which Chromium
 * hands the browser's client ([read]), and the app hands each page what reached it through a function the script
 * keeps on `window` under a name of this page's ([deliver]). Messages go as JSON, so only what JSON holds arrives, and
 * the message ports a message takes along are linked through the app the same way. The page sees the origin of the
 * page that posted the message, and the message only reaches a page of the origin the sender named.
 *
 * Popups the page opens without the user's click or key, or with `noopener`, open as before, without a link.
 */
class OpenerScript {
    private val secret = randomName() + ":"
    private val receiver = "__katari_opener_" + randomName()

    /**
     * The script for the documents of the page, which a popup opened through `window.open` where [hasOpener].
     */
    fun source(hasOpener: Boolean): String = """
        (() => {
            if (window.top !== window) return;
            const post = console.debug.bind(console);
            const stringify = JSON.stringify;
            const parse = JSON.parse;
            const defineProperty = Object.defineProperty;
            const dispatchEvent = EventTarget.prototype.dispatchEvent;
            const setPortHandler = Object.getOwnPropertyDescriptor(MessagePort.prototype, 'onmessage').set;
            const postToPort = MessagePort.prototype.postMessage;
            const NativeMessageEvent = MessageEvent;
            const NativeMessageChannel = MessageChannel;
            const NativeMessagePort = MessagePort;
            const NativeURL = URL;
            const NativeDOMException = DOMException;
            const randomUUID = crypto.randomUUID.bind(crypto);
            const activation = navigator.userActivation;
            const openWindow = window.open;
            const secret = ${JsonPrimitive(secret)};
            const send = (message) => post(secret + stringify(message));

            const targetOriginOf = (target) => {
                const origin = target === undefined ? '/' : String(target);
                if (origin === '*') return origin;
                if (origin === '/') return location.origin;
                try {
                    return new NativeURL(origin).origin;
                } catch (e) {
                    throw new NativeDOMException(
                        "Invalid target origin '" + origin + "' in a call to 'postMessage'.",
                        'SyntaxError',
                    );
                }
            };
            const serialize = (data) => {
                try {
                    return stringify(data === undefined ? null : data) ?? 'null';
                } catch (e) {
                    throw new NativeDOMException('The message could not be cloned.', 'DataCloneError');
                }
            };

            // The ports messages took between this page and another, by id, with this page's end of each
            const ports = new Map();
            const linkPort = (id, port) => {
                ports.set(id, port);
                setPortHandler.call(port, (event) => {
                    try {
                        send({ type: 'portMessage', port: id, data: serialize(event.data) });
                    } catch (e) {
                        // What JSON can't hold doesn't reach the other page
                    }
                });
            };
            const takePorts = (transfer) => {
                const ids = [];
                if (transfer === undefined || transfer === null) return ids;
                for (const item of transfer) {
                    if (!(item instanceof NativeMessagePort)) continue;
                    const id = randomUUID();
                    linkPort(id, item);
                    ids.push(id);
                }
                return ids;
            };
            // The page gets one end of a channel of its own, whose other end the app links to the sender's port
            const givenPorts = new WeakMap();
            const givePorts = (ids) => ids.map((id) => {
                const channel = new NativeMessageChannel();
                linkPort(id, channel.port2);
                givenPorts.set(channel.port1, id);
                return channel.port1;
            });
            // What the page posts through a port it was given goes to the app at once rather than through the other
            // end, which hears it only on a later task: a popup that posts its result and closes itself straight
            // away would otherwise be closed before the result left
            MessagePort.prototype.postMessage = function postMessage(message, transfer) {
                const id = givenPorts.get(this);
                if (id === undefined) return postToPort.apply(this, arguments);
                send({ type: 'portMessage', port: id, data: serialize(message) });
            };
            const postTo = (target, isClosed, data, options, transfer) => {
                const named = options !== null && typeof options === 'object';
                const targetOrigin = targetOriginOf(named ? options.targetOrigin : options);
                const json = serialize(data);
                if (isClosed()) return;
                send({
                    type: 'post',
                    window: target,
                    data: json,
                    targetOrigin,
                    origin: location.origin,
                    ports: takePorts(named ? options.transfer : transfer),
                });
            };

            // The popups this document opened, by id
            const popups = new Map();
            const popupWindow = (id, state) => ({
                get closed() { return state.closed; },
                close() { if (!state.closed) send({ type: 'close', window: id }); },
                focus() {},
                blur() {},
                postMessage(data, options, transfer) {
                    postTo(id, () => state.closed, data, options, transfer);
                },
            });
            const isOn = (value) =>
                value === '' || value === 'yes' || value === 'true' || (parseInt(value, 10) || 0) !== 0;
            const severs = (features) => String(features).split(',').some((feature) => {
                const [key, value = ''] = feature.split('=').map((part) => part.trim().toLowerCase());
                return (key === 'noopener' || key === 'noreferrer') && isOn(value);
            });
            const ownTargets = ['_self', '_parent', '_top'];
            window.open = function open(url, target, features) {
                const name = target === undefined ? '' : String(target);
                if (
                    !activation.isActive ||
                    ownTargets.includes(name.toLowerCase()) ||
                    (features !== undefined && severs(features))
                ) {
                    return openWindow.apply(this, arguments);
                }
                const blank = url === undefined || url === null || String(url) === '';
                let href = 'about:blank';
                if (!blank) {
                    try {
                        href = new NativeURL(String(url), document.baseURI).href;
                    } catch (e) {
                        throw new NativeDOMException(
                            "Unable to open a window with invalid URL '" + url + "'.",
                            'SyntaxError',
                        );
                    }
                }
                if (href.startsWith('javascript:')) return openWindow.apply(this, arguments);
                if (name !== '' && name.toLowerCase() !== '_blank') {
                    for (const [id, state] of popups) {
                        if (state.name !== name || state.closed) continue;
                        if (!blank) send({ type: 'open', window: id, url: href });
                        return state.window;
                    }
                }
                const id = randomUUID();
                const state = { name, closed: false };
                state.window = popupWindow(id, state);
                popups.set(id, state);
                send({ type: 'open', window: id, url: href });
                return state.window;
            };

            let openerClosed = false;
            let opener = null;
            if ($hasOpener) {
                opener = {
                    get closed() { return openerClosed; },
                    close() {},
                    focus() {},
                    blur() {},
                    postMessage(data, options, transfer) {
                        postTo(undefined, () => openerClosed, data, options, transfer);
                    },
                };
                let shownOpener = opener;
                defineProperty(window, 'opener', {
                    get: () => shownOpener,
                    set: (value) => { shownOpener = value; },
                    enumerable: true,
                    configurable: true,
                });
                window.close = function close() { send({ type: 'close' }); };
            }

            defineProperty(window, ${JsonPrimitive(receiver)}, {
                value: (message) => {
                    switch (message.type) {
                        case 'message': {
                            if (message.targetOrigin !== '*' && message.targetOrigin !== location.origin) return;
                            const source = message.window === undefined
                                ? opener
                                : popups.get(message.window)?.window ?? null;
                            const event = new NativeMessageEvent('message', {
                                data: parse(message.data),
                                origin: message.origin,
                                ports: givePorts(message.ports),
                            });
                            defineProperty(event, 'source', { value: source });
                            dispatchEvent.call(window, event);
                            break;
                        }
                        case 'portMessage': {
                            const port = ports.get(message.port);
                            if (port !== undefined) postToPort.call(port, parse(message.data));
                            break;
                        }
                        case 'closed': {
                            const state = popups.get(message.window);
                            if (state !== undefined) state.closed = true;
                            break;
                        }
                        case 'openerClosed':
                            openerClosed = true;
                            break;
                    }
                },
            });
        })();
    """.trimIndent()

    /**
     * The message the console [message] carries; null where it is one of the page's own console messages or can't be
     * read.
     */
    fun read(message: String): OpenerMessage? {
        if (!message.startsWith(secret)) return null
        val fields = runCatching { Json.parseToJsonElement(message.removePrefix(secret)).jsonObject }.getOrNull()
        val read = fields?.let(::messageOf)
        if (read ==
            null
        ) {
            logcat(LogPriority.WARN) { "A page's message to the window it is linked with couldn't be read" }
        }
        return read
    }

    /**
     * The script that hands the page [post], which a popup posted to it as [window], or its opener where [window] is
     * null.
     */
    fun deliver(post: OpenerMessage.Post, window: String?): String = deliver(
        buildJsonObject {
            put("type", "message")
            window?.let { put("window", it) }
            put("data", post.data)
            put("targetOrigin", post.targetOrigin)
            put("origin", post.origin)
            putJsonArray("ports") { post.ports.forEach { add(JsonPrimitive(it)) } }
        },
    )

    fun deliver(post: OpenerMessage.PortPost): String = deliver(
        buildJsonObject {
            put("type", "portMessage")
            put("port", post.port)
            put("data", post.data)
        },
    )

    /** The script that tells the page its popup [window] closed. */
    fun deliverClosed(window: String): String = deliver(
        buildJsonObject {
            put("type", "closed")
            put("window", window)
        },
    )

    fun deliverOpenerClosed(): String = deliver(buildJsonObject { put("type", "openerClosed") })

    // The page may show a document the script didn't run in, such as the blank page a browser starts on
    private fun deliver(message: JsonObject) = "window[${JsonPrimitive(receiver)}]?.($message)"

    private fun messageOf(fields: JsonObject): OpenerMessage? {
        fun string(name: String) = fields[name]?.jsonPrimitive?.contentOrNull
        return when (string("type")) {
            "open" -> OpenerMessage.Open(window = string("window") ?: return null, url = string("url") ?: return null)
            "post" -> OpenerMessage.Post(
                window = string("window"),
                data = string("data") ?: return null,
                targetOrigin = string("targetOrigin") ?: return null,
                origin = string("origin") ?: return null,
                ports = (fields["ports"] as? JsonArray)?.map { it.jsonPrimitive.contentOrNull ?: return null }
                    ?: return null,
            )
            "portMessage" -> {
                val port = string("port") ?: return null
                OpenerMessage.PortPost(port, data = string("data") ?: return null)
            }
            "close" -> OpenerMessage.Close(window = string("window"))
            else -> null
        }
    }

    private companion object {
        const val NAME_BYTES = 16

        fun randomName(): String = ByteArray(NAME_BYTES)
            .also(SecureRandom()::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    }
}
