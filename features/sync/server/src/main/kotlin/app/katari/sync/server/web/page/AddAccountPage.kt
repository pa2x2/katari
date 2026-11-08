package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.InputType
import kotlinx.html.h1
import kotlinx.html.p

internal suspend fun ApplicationCall.respondAddAccountPage(
    error: String? = null,
    username: String = "",
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    respondPage("Add account", status) {
        centreCard {
            h1 { +"Add account" }
            p("lede") {
                +"Set the username and password the person signs in with. They can change the password later. "
                +"To let them choose both, send an invite instead."
            }
            formError(error)
            postForm(NEW_ACCOUNT_PATH, token) {
                field("Username", "username", autocomplete = "off", value = username)
                field("Password", "password", autocomplete = "new-password", type = InputType.password)
                field("Repeat password", "repeat", autocomplete = "new-password", type = InputType.password)
                actions {
                    linkButton("Cancel", "/", ButtonStyle.Text)
                    submitButton("Add account", ButtonStyle.Filled)
                }
            }
        }
    }
}
