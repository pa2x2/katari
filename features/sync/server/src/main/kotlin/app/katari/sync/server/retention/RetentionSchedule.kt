package app.katari.sync.server.retention

import app.katari.sync.server.device.AccountDevices
import app.katari.sync.server.invite.Invites
import app.katari.sync.server.message.DeviceMessages
import app.katari.sync.server.websession.WebSessions
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** Runs every retention sweep when the server starts and every [interval] after. */
class RetentionSchedule(
    private val markers: DeletionMarkerRetention,
    private val files: UnreferencedFileRetention,
    private val devices: AccountDevices,
    private val invites: Invites,
    private val webSessions: WebSessions,
    private val messages: DeviceMessages,
    private val interval: Duration = 1.hours,
) {
    private val log = KtorSimpleLogger("app.katari.sync.server.retention")

    fun start(scope: CoroutineScope): Job = scope.launch(Dispatchers.IO) {
        while (isActive) {
            sweep()
            delay(interval)
        }
    }

    private fun sweep() {
        // A failed sweep only delays removal until the next one, so it must not stop the schedule.
        runCatching {
            markers.sweep()
            files.sweep()
            devices.forgetExpiredLinkCodes()
            invites.forgetExpired()
            webSessions.forgetExpired()
            messages.forgetExpired()
        }.onFailure { log.error("Retention sweep failed", it) }
    }
}
