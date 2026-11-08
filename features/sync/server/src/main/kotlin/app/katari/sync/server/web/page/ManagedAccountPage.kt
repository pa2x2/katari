package app.katari.sync.server.web.page

import app.katari.sync.server.administration.ManagedAccount
import app.katari.sync.server.web.request.visitor
import io.ktor.server.application.ApplicationCall
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.b
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.input
import kotlinx.html.p

/** One account's page for an admin: its devices and everything an admin can do to it. */
internal suspend fun ApplicationCall.respondManagedAccount(managed: ManagedAccount, nowMillis: Long) {
    val token = visitor.formToken
    val account = managed.summary
    val keepsAdmin = managed.isLastEnabledAdmin
    respondPage(account.username) {
        notice(this@respondManagedAccount)
        div("title") {
            h1 { +account.username }
            accountTags(account.isAdmin, account.isDisabled, isYou = account.id == visitor.account?.id)
        }
        p("lede") {
            +"${plural(account.devices, "device")} · last sync ${lastSyncText(account.lastSeenMillis, nowMillis)} · "
            +"${byteSizeText(account.storedBytes)} stored"
        }
        div("card") {
            h2 { +"Devices" }
            deviceTable(
                managed.devices.map { DeviceRow(it.id.value, it.description, it.lastSeenMillis) },
                nowMillis,
                emptyText = "No devices are linked to this account.",
            )
        }
        div("card") {
            h2 { +"Manage" }
            setting("Password", "Give the account a new password, shown once. Its web sign-ins end; devices stay.") {
                postForm(managedAccountActionPath(account.id, ManagedAccountAction.ResetPassword), token) {
                    submitButton("Reset password", ButtonStyle.Outlined)
                }
            }
            val adminText = when {
                !account.isAdmin -> "Admins manage every account on this server."
                keepsAdmin -> "The only enabled admin stays one."
                else -> "Take away managing the server's accounts."
            }
            setting("Admin", adminText) {
                postForm(managedAccountActionPath(account.id, ManagedAccountAction.SetAdmin), token) {
                    input(InputType.hidden, name = "admin") { value = (!account.isAdmin).toString() }
                    val label = if (account.isAdmin) "Remove admin" else "Make admin"
                    submitButton(label, ButtonStyle.Outlined, enabled = !keepsAdmin)
                }
            }
            val accessText = when {
                account.isDisabled -> "Disabled: its devices don't sync and it can't sign in."
                keepsAdmin -> "The only enabled admin can't be disabled."
                else -> "Stop its devices syncing and its sign-ins; its data stays."
            }
            setting("Access", accessText) {
                postForm(managedAccountActionPath(account.id, ManagedAccountAction.SetDisabled), token) {
                    input(InputType.hidden, name = "disabled") { value = (!account.isDisabled).toString() }
                    val label = if (account.isDisabled) "Enable" else "Disable"
                    submitButton(label, ButtonStyle.Outlined, enabled = !keepsAdmin)
                }
            }
            setting("Delete account", "Remove the account, its synced data and files. Its devices keep theirs.") {
                if (keepsAdmin) {
                    submitButton("Delete account", ButtonStyle.Danger, enabled = false)
                } else {
                    linkButton(
                        "Delete account",
                        managedAccountActionPath(account.id, ManagedAccountAction.Delete),
                        ButtonStyle.Danger,
                    )
                }
            }
        }
    }
}

/** A row of a settings card: what it is, what it does, and its control. */
internal fun FlowContent.setting(title: String, description: String, control: FlowContent.() -> Unit) {
    div("setting") {
        div("setting-text") {
            b { +title }
            p("sub") { +description }
        }
        div("setting-control") { control() }
    }
}
