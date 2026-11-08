package app.katari.sync.client.server

import app.katari.sync.protocol.live.ChangesAvailable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * The server's live channel: a WebSocket on which the server announces each new position of the account, the first
 * on connecting. The device sends nothing; OkHttp answers the server's pings and pings itself, so a connection that
 * died without a close ends here as a failure within [OkHttpClient.pingIntervalMillis].
 */
class LiveChannel internal constructor(http: OkHttpClient, request: Request, private val listener: Listener) {
    interface Listener {
        fun onAnnouncement(changes: ChangesAvailable)

        /**
         * The channel ended and won't announce anything more: closed by either side, or failed. [refused] is set when
         * the server refused to open it, which the next call to the server says more about.
         */
        fun onEnded(refused: Boolean)
    }

    private val socket: WebSocket = http.newWebSocket(
        request,
        object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val changes = try {
                    ProtoBuf.decodeFromByteArray(ChangesAvailable.serializer(), bytes.toByteArray())
                } catch (_: SerializationException) {
                    return
                } catch (_: IllegalArgumentException) {
                    return
                }
                listener.onAnnouncement(changes)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onEnded(refused = false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onEnded(refused = response != null)
            }
        },
    )

    fun close() {
        socket.close(NORMAL_CLOSURE, null)
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
