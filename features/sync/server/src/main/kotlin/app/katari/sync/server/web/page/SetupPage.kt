package app.katari.sync.server.web.page

import app.katari.sync.server.web.request.SETUP_PATH
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.InputType
import kotlinx.html.code
import kotlinx.html.h1
import kotlinx.html.p

/** Shown in place of every page until the server has its admin account. */
internal suspend fun ApplicationCall.respondSetupPage(
    error: String? = null,
    username: String = "",
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val token = visitor.formToken
    respondPage("Set up this server", status) {
        centreCard {
            h1 { +"Set up this server" }
            p("lede") { +"Create the admin account. Devices sign in with it, and it manages everyone else's." }
            formError(error)
            postForm(SETUP_PATH, token) {
                field("Setup code", "code", autocomplete = "off", code = true)
                p("hint") {
                    +"Printed in the server log when it starts: "
                    code { +"docker logs katari-sync" }
                }
                field("Username", "username", autocomplete = "username", value = username)
                field("Password", "password", autocomplete = "new-password", type = InputType.password)
                field("Repeat password", "repeat", autocomplete = "new-password", type = InputType.password)
                actions { submitButton("Create admin account", ButtonStyle.Filled) }
            }
        }
    }
}
