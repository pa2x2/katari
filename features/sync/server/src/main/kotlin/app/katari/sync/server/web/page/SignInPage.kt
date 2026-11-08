package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.InputType
import kotlinx.html.h1
import kotlinx.html.p

internal suspend fun ApplicationCall.respondSignInPage(
    error: String? = null,
    username: String = "",
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    respondPage("Sign in", status) {
        centreCard {
            h1 { +"Sign in" }
            p("lede") { +"See your devices and synced data on this server." }
            formError(error)
            postForm(SIGN_IN_PATH, token) {
                field("Username", "username", autocomplete = "username", value = username)
                field("Password", "password", autocomplete = "current-password", type = InputType.password)
                actions { submitButton("Sign in", ButtonStyle.Filled) }
            }
        }
    }
}
