package app.katari.sync.server.web.route

import app.katari.sync.server.SyncServices
import app.katari.sync.server.web.page.ACCOUNT_PATH
import app.katari.sync.server.web.page.SIGN_IN_PATH
import app.katari.sync.server.web.request.visitor
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** The address itself: the account list for admins, everyone else's own page. */
internal fun Route.startRoute(services: SyncServices) {
    get("/") {
        val account = call.visitor.account
        when {
            account == null -> call.respondRedirect(SIGN_IN_PATH)
            account.isAdmin -> call.respondAccountList(services)
            else -> call.respondRedirect(ACCOUNT_PATH)
        }
    }
}
