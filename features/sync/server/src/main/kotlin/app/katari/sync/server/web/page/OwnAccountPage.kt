package app.katari.sync.server.web.page

import app.katari.sync.protocol.device.LinkedDevices
import app.katari.sync.server.account.AccountSummary
import app.katari.sync.server.web.request.visitor
import io.ktor.server.application.ApplicationCall
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p

/** Everyone's own page: their devices, their password, and wiping what they synced. */
internal suspend fun ApplicationCall.respondOwnAccount(
    account: AccountSummary,
    devices: LinkedDevices,
    nowMillis: Long,
) {
    val token = visitor.formToken
    respondPage("My account") {
        notice(this@respondOwnAccount)
        div("title") {
            h1 { +account.username }
            accountTags(account.isAdmin, isDisabled = false, isYou = false)
        }
        p("lede") {
            +"Synced ${byteSizeText(
                account.storedBytes,
            )} · last sync ${lastSyncText(account.lastSeenMillis, nowMillis)}"
        }
        div("card") {
            h2 { +"Devices" }
            deviceTable(
                devices.devices.map { DeviceRow(it.id.value, it.description, it.lastSeenMillis) },
                nowMillis,
                emptyText = "No devices are linked. Sign in from Katari under Settings → Sync.",
            ) { device ->
                postForm(ownDeviceRemovalPath(device.id), token) { submitButton("Remove", ButtonStyle.Danger) }
            }
        }
        div("card") {
            setting("Password", "Your web sign-ins end when it changes; your devices keep syncing.") {
                linkButton("Change password", ACCOUNT_PASSWORD_PATH, ButtonStyle.Outlined)
            }
        }
        div("card") {
            h2 { +"Danger zone" }
            setting(
                "Delete my synced data",
                "Wipe this account's data on the server and remove every device. They keep what they hold.",
            ) {
                linkButton("Delete my synced data", ACCOUNT_SYNCED_DATA_PATH, ButtonStyle.Danger)
            }
        }
    }
}
