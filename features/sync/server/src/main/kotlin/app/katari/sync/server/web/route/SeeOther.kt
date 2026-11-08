package app.katari.sync.server.web.route

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/** After a form's POST: the browser loads [path] with a GET, so reloading it never sends the form again. */
internal suspend fun ApplicationCall.respondSeeOther(path: String) {
    response.headers.append(HttpHeaders.Location, path)
    respond(HttpStatusCode.SeeOther)
}
