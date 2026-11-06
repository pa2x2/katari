package app.katari.sync.server.web.page

import io.ktor.server.application.ApplicationCall
import kotlinx.html.h1
import kotlinx.html.p

/** The invite's code, shown only in the answer that made it: the server keeps nothing it could show again. */
internal suspend fun ApplicationCall.respondInviteReady(code: String) {
    respondPage("Invite ready") {
        centreCard {
            h1 { +"Invite ready" }
            p("lede") {
                +"Give this code to the person. They enter it in Katari under Sync → Sign in → I have an invite, and "
                +"choose their username and password there. It works once, for 1 day."
            }
            secret(code)
            actions { linkButton("Done", "/", ButtonStyle.Filled) }
        }
    }
}
