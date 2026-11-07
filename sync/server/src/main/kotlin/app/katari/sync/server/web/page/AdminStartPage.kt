package app.katari.sync.server.web.page

import app.katari.sync.server.account.AccountSummary
import app.katari.sync.server.web.request.visitor
import io.ktor.server.application.ApplicationCall
import kotlinx.html.FlowContent
import kotlinx.html.ThScope
import kotlinx.html.a
import kotlinx.html.b
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.span
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

/** The admin's start page: the server at a glance and every account. */
internal suspend fun ApplicationCall.respondAdminStart(
    accounts: List<AccountSummary>,
    version: String,
    nowMillis: Long,
) {
    val visitor = visitor
    val me = checkNotNull(visitor.account)
    respondPage("Accounts") {
        notice(this@respondAdminStart)
        div("stats") {
            stat("Server", version)
            stat("Storage", byteSizeText(accounts.sumOf { it.storedBytes }))
            stat("Accounts", accounts.size.toString())
            stat("Devices", accounts.sumOf { it.devices }.toString())
        }
        div("card") {
            div("card-head") {
                h2 { +"Accounts" }
                div("card-actions") {
                    linkButton("Add account", NEW_ACCOUNT_PATH, ButtonStyle.Outlined)
                    postForm(INVITES_PATH, visitor.formToken) { submitButton("Invite", ButtonStyle.Filled) }
                }
            }
            table("list accounts") {
                thead {
                    tr {
                        th(ThScope.col) { +"Account" }
                        th(ThScope.col) { +"Devices" }
                        th(ThScope.col) { +"Last sync" }
                        th(ThScope.col) { +"Storage" }
                    }
                }
                tbody {
                    accounts.forEach { account ->
                        tr {
                            td("name") {
                                a(href = managedAccountPath(account.id)) { +account.username }
                                accountTags(account.isAdmin, account.isDisabled, isYou = account.id == me.id)
                            }
                            td("num") {
                                attributes["data-label"] = "Devices"
                                +account.devices.toString()
                            }
                            td {
                                attributes["data-label"] = "Last sync"
                                +lastSyncText(account.lastSeenMillis, nowMillis)
                            }
                            td("num") {
                                attributes["data-label"] = "Storage"
                                +byteSizeText(account.storedBytes)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.stat(label: String, value: String) {
    div("stat") {
        span("sub") { +label }
        b { +value }
    }
}

internal fun FlowContent.accountTags(isAdmin: Boolean, isDisabled: Boolean, isYou: Boolean) {
    if (isAdmin) span("tag") { +"admin" }
    if (isDisabled) span("tag muted") { +"disabled" }
    if (isYou) span("tag muted") { +"you" }
}
