package app.katari.sync.server.web.route

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.server.SyncServices
import app.katari.sync.server.account.PasswordChangeOutcome
import app.katari.sync.server.account.WipeOutcome
import app.katari.sync.server.http.clientAddress
import app.katari.sync.server.http.group
import app.katari.sync.server.web.page.ACCOUNT_PASSWORD_PATH
import app.katari.sync.server.web.page.ACCOUNT_PATH
import app.katari.sync.server.web.page.ACCOUNT_SYNCED_DATA_PATH
import app.katari.sync.server.web.page.FormMessages
import app.katari.sync.server.web.page.Notice
import app.katari.sync.server.web.page.respondChangePasswordPage
import app.katari.sync.server.web.page.respondDeleteSyncedDataPage
import app.katari.sync.server.web.page.respondNotFound
import app.katari.sync.server.web.page.respondOwnAccount
import app.katari.sync.server.web.request.form
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Every signed-in person's own account: their devices, password and synced data. */
internal fun Route.ownAccountRoutes(services: SyncServices) {
    group("own account") {
        install(RequireSignIn)

        get(ACCOUNT_PATH) {
            val account = call.signedIn
            val summary = storage { services.storage.accounts.summary(account.id) } ?: return@get call.respondNotFound()
            val devices = storage { services.devices.list(account.id) }
            call.respondOwnAccount(summary, devices, services.clock.now().toEpochMilliseconds())
        }
        post("/account/devices/{id}/remove") {
            val device = call.parameters["id"]?.toLongOrNull()?.let(::DeviceId) ?: return@post call.respondNotFound()
            val removed = storage { services.devices.revoke(call.signedIn.id, device) }
            if (removed) call.respondSeeOther(Notice.DeviceRemoved.path(ACCOUNT_PATH)) else call.respondNotFound()
        }

        get(ACCOUNT_PASSWORD_PATH) { call.respondChangePasswordPage() }
        post(ACCOUNT_PASSWORD_PATH) {
            val password = call.form["password"].orEmpty()
            if (password != call.form["repeat"]) {
                return@post call.respondChangePasswordPage(FormMessages.PASSWORDS_DIFFER, HttpStatusCode.BadRequest)
            }
            val from = call.clientAddress(services.config.trustedProxies)
            val outcome = storage {
                services.passwordChange.change(call.signedIn, call.form["current"].orEmpty(), password, from)
            }
            when (outcome) {
                // Every session ended with the change, this one too; the browser gets a new one.
                PasswordChangeOutcome.Changed ->
                    call.signIn(services, call.signedIn.id, then = Notice.PasswordChanged.path(ACCOUNT_PATH))
                PasswordChangeOutcome.WrongPassword ->
                    call.respondChangePasswordPage(FormMessages.WRONG_PASSWORD, HttpStatusCode.Forbidden)
                PasswordChangeOutcome.InvalidPassword ->
                    call.respondChangePasswordPage(FormMessages.PASSWORD_RULE, HttpStatusCode.BadRequest)
                is PasswordChangeOutcome.Throttled -> call.respondChangePasswordPage(
                    FormMessages.tooManyAttempts(outcome.retryAfter),
                    HttpStatusCode.TooManyRequests,
                )
            }
        }

        get(ACCOUNT_SYNCED_DATA_PATH) { call.respondDeleteSyncedDataPage() }
        post(ACCOUNT_SYNCED_DATA_PATH) {
            val from = call.clientAddress(services.config.trustedProxies)
            val outcome = storage { services.dataWipe.wipe(call.signedIn, call.form["password"].orEmpty(), from) }
            when (outcome) {
                WipeOutcome.Wiped -> call.respondSeeOther(Notice.SyncedDataDeleted.path(ACCOUNT_PATH))
                WipeOutcome.WrongPassword ->
                    call.respondDeleteSyncedDataPage(FormMessages.WRONG_PASSWORD, HttpStatusCode.Forbidden)
                is WipeOutcome.Throttled -> call.respondDeleteSyncedDataPage(
                    FormMessages.tooManyAttempts(outcome.retryAfter),
                    HttpStatusCode.TooManyRequests,
                )
            }
        }
    }
}

private val ApplicationCall.signedIn
    get() = checkNotNull(visitor.account)
