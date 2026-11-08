package app.katari.sync.server.web.request

import app.katari.sync.server.SyncServices
import app.katari.sync.server.web.page.respondFormExpired
import app.katari.sync.server.web.page.respondSetupPage
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val SETUP_PATH = "/setup"

/**
 * Runs before every web page route, so none can skip it: finds the visitor ([WebVisitor]), refuses any POST whose form
 * token is not the visitor's ([FormTokens]) before its handler runs, and shows the setup page in place of every page
 * until the server has its admin account.
 */
internal fun webRequests(services: SyncServices): RouteScopedPlugin<Unit> =
    createRouteScopedPlugin("WebRequests") {
        onCall { call ->
            WebSecurityHeaders.apply(call)
            val visitor = withContext(Dispatchers.IO) { call.findVisitor(services) }
            call.attributes.put(WebVisitorKey, visitor)
            val isPost = call.request.httpMethod == HttpMethod.Post
            if (isPost) {
                val form = call.readForm()
                call.attributes.put(FormKey, form)
                if (!FormTokens.matches(form[FormTokens.FIELD], visitor.formToken)) {
                    call.respondFormExpired()
                    return@onCall
                }
            }
            val settingUp = isPost && call.request.path() == SETUP_PATH
            if (services.firstAdminSetup.setupCode != null && !settingUp) call.respondSetupPage()
        }
    }

private fun ApplicationCall.findVisitor(services: SyncServices): WebVisitor {
    val trustedProxies = services.config.trustedProxies
    val sessionToken = request.cookies[WebCookies.SESSION]?.takeIf(String::isNotEmpty)
    if (sessionToken != null) {
        val account = services.webSessions.accountOf(sessionToken)
        if (account != null) return WebVisitor(account, sessionToken, FormTokens.forSession(sessionToken))
        WebCookies.clearSession(this, trustedProxies)
    }
    val formToken = request.cookies[WebCookies.FORM]?.takeIf(String::isNotEmpty)
        ?: FormTokens.newAnonymous(services.random).also { WebCookies.setForm(this, it, trustedProxies) }
    return WebVisitor(account = null, sessionToken = null, formToken = formToken)
}
