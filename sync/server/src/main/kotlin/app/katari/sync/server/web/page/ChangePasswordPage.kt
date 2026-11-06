package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.InputType
import kotlinx.html.h1
import kotlinx.html.p

internal suspend fun ApplicationCall.respondChangePasswordPage(
    error: String? = null,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    val username = visitor.account?.username
    respondPage("Change password", status) {
        centreCard {
            h1 { +"Change password" }
            p("lede") { +"Every other web sign-in of this account ends. Your devices keep syncing." }
            formError(error)
            postForm(ACCOUNT_PASSWORD_PATH, token) {
                // Lets password managers file the new password under the right account.
                username?.let { hiddenUsername(it) }
                field("Current password", "current", autocomplete = "current-password", type = InputType.password)
                field("New password", "password", autocomplete = "new-password", type = InputType.password)
                field("Repeat new password", "repeat", autocomplete = "new-password", type = InputType.password)
                actions {
                    linkButton("Cancel", ACCOUNT_PATH, ButtonStyle.Text)
                    submitButton("Change password", ButtonStyle.Filled)
                }
            }
        }
    }
}
