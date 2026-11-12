package app.katari.sync.client.message

import app.katari.sync.protocol.message.DeviceMessage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Acts on the messages of one [kind] that the account's other devices send here ([DeviceMessaging]). A message can
 * come again, as when this device stopped before telling the server it had it, so handling it twice must do no harm.
 */
interface MessageHandler {
    val kind: String

    /** The newest format of [kind] this reads; a message in a newer one is dropped, as one that came too late is. */
    val version: Int

    suspend fun handle(message: DeviceMessage)
}

/** Features that take messages contribute their handlers; a graph may have none. */
@ContributesTo(AppScope::class)
interface MessageHandlerBindings {
    @Multibinds(allowEmpty = true)
    fun messageHandlers(): Set<MessageHandler>
}
