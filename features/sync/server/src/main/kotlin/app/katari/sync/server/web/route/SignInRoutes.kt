package app.katari.sync.server.web.route

import app.katari.sync.server.SyncServices
import app.katari.sync.server.account.PasswordCheckOutcome
import app.katari.sync.server.http.clientAddress
import app.katari.sync.server.web.page.FormMessages
import app.katari.sync.server.web.page.SIGN_IN_PATH
import app.katari.sync.server.web.page.SIGN_OUT_PATH
import app.katari.sync.server.web.page.respondSignInPage
import app.katari.sync.server.web.request.WebCookies
import app.katari.sync.server.web.request.form
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Route.signInRoutes(services: SyncServices) {
    get(SIGN_IN_PATH) {
        if (call.visitor.account != null) call.respondRedirect("/") else call.respondSignInPage()
    }
    post(SIGN_IN_PATH) {
        val username = call.form["username"].orEmpty()
        val password = call.form["password"].orEmpty()
        val from = call.clientAddress(services.config.trustedProxies)
        when (val check = withContext(Dispatchers.IO) { services.passwordCheck.check(username, password, from) }) {
            is PasswordCheckOutcome.Matched -> if (check.account.isDisabled) {
                call.respondSignInPage(
                    "This account is disabled. Ask the server's admin.",
                    username,
                    HttpStatusCode.Forbidden,
                )
            } else {
                call.signIn(services, check.account.id)
            }
            PasswordCheckOutcome.Wrong ->
                call.respondSignInPage("Wrong username or password.", username, HttpStatusCode.Unauthorized)
            is PasswordCheckOutcome.Throttled -> call.respondSignInPage(
                FormMessages.tooManyAttempts(check.retryAfter),
                username,
                HttpStatusCode.TooManyRequests,
            )
        }
    }
    post(SIGN_OUT_PATH) {
        call.visitor.sessionToken?.let { withContext(Dispatchers.IO) { services.webSessions.end(it) } }
        WebCookies.clearSession(call, services.config.trustedProxies)
        call.respondSeeOther(SIGN_IN_PATH)
    }
}

/**
 * Starts a new web session for [accountId] in this browser, ending the one it had, and goes to [then]. A new token on
 * every sign-in, so a session token planted in the browser beforehand never becomes a signed-in one.
 */
internal suspend fun ApplicationCall.signIn(services: SyncServices, accountId: Long, then: String = "/") {
    val previous = visitor.sessionToken
    val token = withContext(Dispatchers.IO) {
        previous?.let(services.webSessions::end)
        services.webSessions.start(accountId)
    }
    WebCookies.setSession(this, token, services.config.trustedProxies)
    respondSeeOther(then)
}
