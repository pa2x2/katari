package app.katari.sync.client.message

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * The [MessageHandler]s of this app, by kind: the inbox hands each message to its kind's, and the account's device list
 * says which kinds this device takes, so another device offers to send it only those.
 */
@Inject
@SingleIn(AppScope::class)
class MessageHandlers(
    // Taken when first asked: a handler may send messages of its own, and sending needs the runs that need these
    handlers: Lazy<Set<MessageHandler>>,
) {
    private val byKind by lazy { handlers.value.associateBy { it.kind } }

    fun of(kind: String): MessageHandler? = byKind[kind]

    /** The kinds this device acts on, in a steady order so its description changes only when they do. */
    fun kinds(): List<String> = byKind.keys.sorted()
}
