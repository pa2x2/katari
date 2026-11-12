package app.katari.sync.ui.testmessage

import app.katari.sync.client.account.AccountDevice
import app.katari.sync.client.account.DeviceDirectory
import app.katari.sync.client.message.DeviceMessaging
import app.katari.sync.client.message.MessageHandler
import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.message.MessageBody
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.time.Duration.Companion.hours

/**
 * A message that only says it came, which debug builds send from the account's device list to try messages between
 * devices; the device it reaches tells by notification.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class TestMessages(
    private val messaging: DeviceMessaging,
    private val notifications: SyncNotifications,
    private val directory: DeviceDirectory,
) : MessageHandler {
    override val kind: String = "test"

    override val version: Int = 1

    suspend fun send(to: AccountDevice) {
        messaging.send(to.id, MessageBody(kind, version), lifetime = 1.hours)
    }

    override suspend fun handle(message: DeviceMessage) {
        notifications.showTestMessage(from = directory.listedName(message.from))
    }
}
