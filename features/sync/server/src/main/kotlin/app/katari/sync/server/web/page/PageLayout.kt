package app.katari.sync.server.web.page

import app.katari.sync.server.ServerVersion
import app.katari.sync.server.web.request.FormTokens
import app.katari.sync.server.web.request.WebVisitor
import app.katari.sync.server.web.request.visitor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.MAIN
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.img
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.title

/** Where the stylesheet, script, font and logo are served from; the server's resources under `web/`. */
internal const val ASSETS_PATH = "/assets"

/** The website's guide to running the server: setup, accounts, HTTPS and updating. */
private const val GUIDE_URL = "https://pa2x2.github.io/katari/docs/sync-server/"

internal suspend fun ApplicationCall.respondPage(
    title: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    content: MAIN.() -> Unit,
) {
    val visitor = visitor
    respondHtml(status) { katariPage(title, visitor, content) }
}

private fun HTML.katariPage(title: String, visitor: WebVisitor, content: MAIN.() -> Unit) {
    lang = "en"
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        meta(name = "color-scheme", content = "light dark")
        title("$title · Katari sync")
        link(rel = "stylesheet", href = "$ASSETS_PATH/katari.css")
        link(rel = "icon", href = "$ASSETS_PATH/logo.svg", type = "image/svg+xml")
        script(src = "$ASSETS_PATH/katari.js") { defer = true }
    }
    body {
        header("top") {
            a(href = "/", classes = "brand") {
                img(src = "$ASSETS_PATH/logo.svg", alt = "", classes = "logo")
                span("nm") { +"Katari sync" }
            }
            visitor.account?.let { account ->
                nav("who") {
                    span("me") {
                        +account.username
                        if (account.isAdmin) +" · admin"
                    }
                    if (account.isAdmin) a(href = "/", classes = "bt t") { +"Accounts" }
                    a(href = ACCOUNT_PATH, classes = "bt t") { +"My account" }
                    postForm(SIGN_OUT_PATH, FormTokens.forSession(checkNotNull(visitor.sessionToken))) {
                        submitButton("Sign out", ButtonStyle.Text)
                    }
                }
            }
        }
        main("body") { content() }
        footer("foot") {
            // Admins, who upgrade the server, see its version; nobody else is told which version runs.
            if (visitor.account?.isAdmin == true) +"Katari sync server ${ServerVersion.name} · "
            a(href = GUIDE_URL, target = "_blank") {
                attributes["rel"] = "noopener"
                +"Help"
            }
        }
    }
}

/** A narrow card centred on the page, for one form or message. */
internal fun FlowContent.centreCard(content: FlowContent.() -> Unit) {
    div("card center") { content() }
}
