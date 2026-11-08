package app.katari.sync.server.web.route

import app.katari.sync.server.SyncServices
import app.katari.sync.server.administration.AdminOutcome
import app.katari.sync.server.http.browserAddress
import app.katari.sync.server.http.group
import app.katari.sync.server.web.page.FormMessages
import app.katari.sync.server.web.page.INVITES_PATH
import app.katari.sync.server.web.page.ManagedAccountAction
import app.katari.sync.server.web.page.NEW_ACCOUNT_PATH
import app.katari.sync.server.web.page.Notice
import app.katari.sync.server.web.page.managedAccountPath
import app.katari.sync.server.web.page.respondAddAccountPage
import app.katari.sync.server.web.page.respondAdminStart
import app.katari.sync.server.web.page.respondDeleteAccountPage
import app.katari.sync.server.web.page.respondInviteReady
import app.katari.sync.server.web.page.respondLastAdmin
import app.katari.sync.server.web.page.respondManagedAccount
import app.katari.sync.server.web.page.respondNotAllowed
import app.katari.sync.server.web.page.respondNotFound
import app.katari.sync.server.web.page.respondPasswordReset
import app.katari.sync.server.web.request.form
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post

internal fun Route.adminRoutes(services: SyncServices) {
    val administration = services.administration
    group("admin") {
        install(RequireAdmin)

        get(NEW_ACCOUNT_PATH) { call.respondAddAccountPage() }
        post(NEW_ACCOUNT_PATH) {
            val username = call.form["username"].orEmpty()
            val password = call.form["password"].orEmpty()
            if (password != call.form["repeat"]) {
                return@post call.respondAddAccountPage(
                    FormMessages.PASSWORDS_DIFFER,
                    username,
                    HttpStatusCode.BadRequest,
                )
            }
            when (val outcome = storage { administration.add(call.actorId, username, password) }) {
                is AdminOutcome.Done -> call.respondSeeOther(Notice.AccountAdded.path("/"))
                AdminOutcome.UsernameTaken ->
                    call.respondAddAccountPage(FormMessages.USERNAME_TAKEN, username, HttpStatusCode.Conflict)
                AdminOutcome.InvalidUsername ->
                    call.respondAddAccountPage(FormMessages.USERNAME_RULE, username, HttpStatusCode.BadRequest)
                AdminOutcome.InvalidPassword ->
                    call.respondAddAccountPage(FormMessages.PASSWORD_RULE, username, HttpStatusCode.BadRequest)
                else -> call.respondRefusal(outcome)
            }
        }

        post(INVITES_PATH) {
            val code = storage { services.invites.issue(call.actorId) }
            if (code == null) {
                call.respondNotAllowed()
            } else {
                call.respondInviteReady(code, call.browserAddress(services.config.trustedProxies))
            }
        }

        get("/accounts/{id}") {
            val id = call.accountId() ?: return@get call.respondNotFound()
            when (val outcome = storage { administration.account(call.actorId, id) }) {
                is AdminOutcome.Done -> call.respondManagedAccount(
                    outcome.value,
                    services.clock.now().toEpochMilliseconds(),
                )
                else -> call.respondRefusal(outcome)
            }
        }
        post(action(ManagedAccountAction.ResetPassword)) {
            val id = call.accountId() ?: return@post call.respondNotFound()
            when (val outcome = storage { administration.resetPassword(call.actorId, id) }) {
                is AdminOutcome.Done -> {
                    val username = storage { services.storage.accounts.byId(id)?.username }.orEmpty()
                    call.respondPasswordReset(id, username, outcome.value)
                }
                else -> call.respondRefusal(outcome)
            }
        }
        post(action(ManagedAccountAction.SetAdmin)) {
            val id = call.accountId() ?: return@post call.respondNotFound()
            val admin = call.form["admin"].toBoolean()
            when (val outcome = storage { administration.setAdmin(call.actorId, id, admin) }) {
                // An admin who just demoted themselves has no account pages left to see.
                is AdminOutcome.Done -> call.respondSeeOther(
                    if (!admin && id == call.actorId) {
                        "/"
                    } else {
                        (if (admin) Notice.AdminMade else Notice.AdminRemoved)
                            .path(managedAccountPath(id))
                    },
                )
                else -> call.respondRefusal(outcome)
            }
        }
        post(action(ManagedAccountAction.SetDisabled)) {
            val id = call.accountId() ?: return@post call.respondNotFound()
            val disabled = call.form["disabled"].toBoolean()
            when (val outcome = storage { administration.setDisabled(call.actorId, id, disabled) }) {
                is AdminOutcome.Done -> call.respondSeeOther(
                    (if (disabled) Notice.AccountDisabled else Notice.AccountEnabled).path(managedAccountPath(id)),
                )
                else -> call.respondRefusal(outcome)
            }
        }
        get(action(ManagedAccountAction.Delete)) {
            val id = call.accountId() ?: return@get call.respondNotFound()
            when (val outcome = storage { administration.account(call.actorId, id) }) {
                is AdminOutcome.Done -> if (outcome.value.isLastEnabledAdmin) {
                    call.respondLastAdmin()
                } else {
                    call.respondDeleteAccountPage(outcome.value.summary)
                }
                else -> call.respondRefusal(outcome)
            }
        }
        post(action(ManagedAccountAction.Delete)) {
            val id = call.accountId() ?: return@post call.respondNotFound()
            when (val outcome = storage { administration.delete(call.actorId, id, call.form["username"].orEmpty()) }) {
                is AdminOutcome.Done -> call.respondSeeOther(Notice.AccountDeleted.path("/"))
                AdminOutcome.NotConfirmed -> {
                    val account =
                        storage { services.storage.accounts.summary(id) } ?: return@post call.respondNotFound()
                    call.respondDeleteAccountPage(
                        account,
                        "That isn't this account's username, so nothing was deleted.",
                        HttpStatusCode.BadRequest,
                    )
                }
                else -> call.respondRefusal(outcome)
            }
        }
    }
}

/** The admin start page, at the address itself ([startRoute]). */
internal suspend fun ApplicationCall.respondAccountList(services: SyncServices) {
    when (val outcome = storage { services.administration.accounts(actorId) }) {
        is AdminOutcome.Done -> respondAdminStart(outcome.value, services.clock.now().toEpochMilliseconds())
        else -> respondRefusal(outcome)
    }
}

private fun action(action: ManagedAccountAction): String = "/accounts/{id}/${action.segment}"

private val ApplicationCall.actorId: Long
    get() = checkNotNull(visitor.account).id

private fun RoutingCall.accountId(): Long? = parameters["id"]?.toLongOrNull()

private suspend fun ApplicationCall.respondRefusal(outcome: AdminOutcome<*>) {
    when (outcome) {
        AdminOutcome.NoSuchAccount -> respondNotFound()
        AdminOutcome.LastAdmin -> respondLastAdmin()
        else -> respondNotAllowed()
    }
}
