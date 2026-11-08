package app.katari.sync.server.web.page

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.h1
import kotlinx.html.p

/** A POST without the visitor's form token: from another site, or a page left open past its session. */
internal suspend fun ApplicationCall.respondFormExpired() {
    respondMessage(
        "Page expired",
        "This page was open too long or came from somewhere else, so nothing was changed. Open it again and retry.",
        HttpStatusCode.Forbidden,
    )
}

internal suspend fun ApplicationCall.respondNotAllowed() {
    respondMessage("Not allowed", "Only the server's admins can do this.", HttpStatusCode.Forbidden)
}

internal suspend fun ApplicationCall.respondNotFound() {
    respondMessage("Not found", "There is nothing here. It may have been removed.", HttpStatusCode.NotFound)
}

internal suspend fun ApplicationCall.respondLastAdmin() {
    respondMessage(
        "The last admin stays",
        "This is the only enabled admin, so it can't be demoted, disabled or deleted. Make another account an admin " +
            "first.",
        HttpStatusCode.Conflict,
    )
}

private suspend fun ApplicationCall.respondMessage(title: String, message: String, status: HttpStatusCode) {
    respondPage(title, status) {
        centreCard {
            h1 { +title }
            p("lede") { +message }
            actions { linkButton("Back to the start", "/", ButtonStyle.Filled) }
        }
    }
}
