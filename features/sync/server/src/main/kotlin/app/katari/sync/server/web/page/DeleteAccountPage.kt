package app.katari.sync.server.web.page

import app.katari.sync.server.account.AccountSummary
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.h1
import kotlinx.html.p

internal suspend fun ApplicationCall.respondDeleteAccountPage(
    account: AccountSummary,
    error: String? = null,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    respondPage("Delete ${account.username}", status) {
        centreCard {
            h1 { +"Delete ${account.username}?" }
            p("lede") {
                +"This removes the account and everything it synced to this server, records and files. "
                +"Its ${plural(account.devices, "device")} stop syncing and keep everything they hold."
            }
            formError(error)
            postForm(managedAccountActionPath(account.id, ManagedAccountAction.Delete), token) {
                field("Type ${account.username} to confirm", "username", autocomplete = "off")
                actions {
                    linkButton("Cancel", managedAccountPath(account.id), ButtonStyle.Text)
                    submitButton("Delete account", ButtonStyle.DangerFilled)
                }
            }
        }
    }
}
