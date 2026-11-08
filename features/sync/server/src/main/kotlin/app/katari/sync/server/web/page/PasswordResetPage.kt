package app.katari.sync.server.web.page

import io.ktor.server.application.ApplicationCall
import kotlinx.html.h1
import kotlinx.html.p

/** The new password, shown only in the answer that set it: the server keeps only its hash. */
internal suspend fun ApplicationCall.respondPasswordReset(accountId: Long, username: String, password: String) {
    respondPage("New password") {
        centreCard {
            h1 { +"New password for $username" }
            p("lede") {
                +"Give it to $username; it is shown only this once. Their web sign-ins have ended, and their devices "
                +"keep syncing."
            }
            secret(password)
            actions { linkButton("Done", managedAccountPath(accountId), ButtonStyle.Filled) }
        }
    }
}
