package app.katari.sync.server.web.route

import app.katari.sync.server.SyncServices
import app.katari.sync.server.account.FirstAdminOutcome
import app.katari.sync.server.http.clientAddress
import app.katari.sync.server.web.page.FormMessages
import app.katari.sync.server.web.page.respondSetupPage
import app.katari.sync.server.web.request.SETUP_PATH
import app.katari.sync.server.web.request.form
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Until setup, every page is the setup page ([app.katari.sync.server.web.request.webRequests]); this takes its form. */
internal fun Route.setupRoutes(services: SyncServices) {
    get(SETUP_PATH) { call.respondRedirect("/") }
    post(SETUP_PATH) {
        val form = call.form
        val username = form["username"].orEmpty()
        val password = form["password"].orEmpty()
        if (password != form["repeat"]) {
            return@post call.respondSetupPage(FormMessages.PASSWORDS_DIFFER, username, HttpStatusCode.BadRequest)
        }
        val from = call.clientAddress(services.config.trustedProxies)
        val outcome = withContext(Dispatchers.IO) {
            services.firstAdminSetup.createAdmin(form["code"].orEmpty(), username, password, from)
        }
        when (outcome) {
            is FirstAdminOutcome.Created -> call.signIn(services, outcome.account.id)
            FirstAdminOutcome.AlreadySetUp -> call.respondSeeOther("/")
            FirstAdminOutcome.WrongCode -> call.respondSetupPage(
                "That setup code isn't right. Check the server log; a restart prints a new one.",
                username,
                HttpStatusCode.Forbidden,
            )
            is FirstAdminOutcome.Throttled -> call.respondSetupPage(
                FormMessages.tooManyAttempts(outcome.retryAfter),
                username,
                HttpStatusCode.TooManyRequests,
            )
            FirstAdminOutcome.InvalidUsername ->
                call.respondSetupPage(FormMessages.USERNAME_RULE, username, HttpStatusCode.BadRequest)
            FirstAdminOutcome.InvalidPassword ->
                call.respondSetupPage(FormMessages.PASSWORD_RULE, username, HttpStatusCode.BadRequest)
        }
    }
}
