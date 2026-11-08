package app.katari.sync.server.web.page

import io.ktor.server.application.ApplicationCall
import kotlinx.html.FlowContent
import kotlinx.html.p
import kotlinx.html.role

/**
 * What a page says after an action redirected to it. Only a key travels in the address (`?done=…`), never the text,
 * so nothing a link carries is written into the page.
 */
internal enum class Notice(val key: String, val text: String) {
    AccountAdded("account-added", "Account added. It can sign in now."),
    AccountDeleted("account-deleted", "Account deleted. Its devices carry on without sync, keeping what they hold."),
    AdminMade("admin-made", "The account is an admin now."),
    AdminRemoved("admin-removed", "The account is no longer an admin."),
    AccountDisabled("disabled", "Account disabled. Its devices stop syncing and keep their data."),
    AccountEnabled("enabled", "Account enabled. Its devices sync again."),
    DeviceRemoved("device-removed", "Device removed. It stops syncing and keeps everything it holds."),
    PasswordChanged("password-changed", "Password changed. Every other web sign-in has ended."),
    SyncedDataDeleted(
        "synced-data-deleted",
        "Your synced data is deleted from this server and your devices are removed. They keep everything they hold.",
    ),
    ;

    fun path(base: String): String = "$base?done=$key"
}

internal fun FlowContent.notice(call: ApplicationCall) {
    val key = call.request.queryParameters["done"] ?: return
    val notice = Notice.entries.firstOrNull { it.key == key } ?: return
    p("notice") {
        role = "status"
        +notice.text
    }
}
