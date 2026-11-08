package app.katari.sync.server.web.route

import app.katari.sync.server.web.page.SIGN_IN_PATH
import app.katari.sync.server.web.page.respondNotAllowed
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpMethod
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respondRedirect

/** Sends visitors who aren't signed in to the sign-in page, and refuses what they POST. */
internal val RequireSignIn = createRouteScopedPlugin("RequireSignIn") {
    onCall { call ->
        if (call.visitor.account != null) return@onCall
        if (call.request.httpMethod == HttpMethod.Get) call.respondRedirect(SIGN_IN_PATH) else call.respondNotAllowed()
    }
}

/**
 * Lets only admins through, in front of every admin route. The services check again with the account as stored when
 * the action runs, so this is what keeps admin pages from rendering, not what keeps actions from happening.
 */
internal val RequireAdmin = createRouteScopedPlugin("RequireAdmin") {
    onCall { call ->
        val account = call.visitor.account
        when {
            account?.isAdmin == true -> Unit
            account == null && call.request.httpMethod == HttpMethod.Get -> call.respondRedirect(SIGN_IN_PATH)
            else -> call.respondNotAllowed()
        }
    }
}
