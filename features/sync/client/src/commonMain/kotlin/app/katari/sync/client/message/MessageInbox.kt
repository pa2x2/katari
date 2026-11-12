package app.katari.sync.client.message

import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.live.ChangesAvailable
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.FetchMessagesRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Hands the messages the account's other devices sent here to the [MessageHandler] of their kind, in the order they
 * were sent. The number of the last one handled stays with the connection and only later ones are new, so a message
 * that comes again, along with an announcement and then in a fetch, is handled once. Few and small messages come with
 * the live channel's announcement; the rest are fetched. Either way, the fetch after the last one handled is what tells
 * the server they arrived. A message whose kind nothing here handles, in a format too new, or that expired, is passed
 * over.
 */
@Inject
@SingleIn(AppScope::class)
class MessageInbox(
    private val connection: SyncConnection,
    private val handlers: MessageHandlers,
) {
    private val handling = Mutex()

    /** Handles what came along with [changes], then fetches the rest, when it announces any message not handled yet. */
    internal suspend fun announced(changes: ChangesAvailable, client: SyncServerClient) {
        if (changes.newestMessage <= connection.state().messagesHandled) return
        handle(changes.messages)
        fetch(client)
    }

    /**
     * Fetches and handles every message waiting. With a [wait], one fetch that the server holds back until a message
     * comes or the wait runs out, which the next fetch confirms.
     */
    internal suspend fun fetch(client: SyncServerClient, wait: Duration = Duration.ZERO) {
        do {
            val request = FetchMessagesRequest(after = handled(), waitMillis = wait.inWholeMilliseconds)
            val messages = client.fetchMessages(request).messages
            handle(messages)
        } while (messages.isNotEmpty() && !wait.isPositive())
    }

    private suspend fun handled() = connection.state().messagesHandled

    private suspend fun handle(messages: List<DeviceMessage>) = handling.withLock {
        var handled = handled()
        messages.sortedBy { it.number }.filter { it.number > handled }.forEach { message ->
            if (message.expiresAtMillis > Clock.System.now().toEpochMilliseconds()) dispatch(message)
            handled = message.number
            connection.messagesHandled(handled)
        }
    }

    private suspend fun dispatch(message: DeviceMessage) {
        val handler = handlers.of(message.body.kind)
        if (handler == null || message.body.kindVersion > handler.version) {
            logcat(LogPriority.INFO) { "Passed over a ${message.body.kind} message (${message.body.kindVersion})" }
            return
        }
        try {
            handler.handle(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A handler that fails would fail again on every retry; the messages after it still go to theirs.
            logcat(LogPriority.ERROR, e) { "A ${message.body.kind} message wasn't handled" }
        }
    }
}
