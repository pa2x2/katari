package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.InputType
import kotlinx.html.h1
import kotlinx.html.p

internal suspend fun ApplicationCall.respondDeleteSyncedDataPage(
    error: String? = null,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    val username = visitor.account?.username
    respondPage("Delete my synced data", status) {
        centreCard {
            h1 { +"Delete my synced data?" }
            p("lede") {
                +"This removes everything this account has synced to the server, records and files, and removes "
                +"all its devices. They stop syncing and keep everything they hold. Your account stays, so you can "
                +"link devices again and start afresh."
            }
            formError(error)
            postForm(ACCOUNT_SYNCED_DATA_PATH, token) {
                username?.let { hiddenUsername(it) }
                field("Password", "password", autocomplete = "current-password", type = InputType.password)
                actions {
                    linkButton("Cancel", ACCOUNT_PATH, ButtonStyle.Text)
                    submitButton("Delete my synced data", ButtonStyle.DangerFilled)
                }
            }
        }
    }
}
