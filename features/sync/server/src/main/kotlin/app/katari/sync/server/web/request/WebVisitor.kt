package app.katari.sync.server.web.request

import app.katari.sync.server.account.Account
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

/** Who is looking at a web page, as [webRequests] found out before any route runs. */
internal class WebVisitor(
    /** Signed in and enabled now; null for anyone else. */
    val account: Account?,
    val sessionToken: String?,
    /** What every form on the page carries, and what a POST from this visitor must carry ([FormTokens]). */
    val formToken: String,
)

internal val WebVisitorKey = AttributeKey<WebVisitor>("WebVisitor")

internal val ApplicationCall.visitor: WebVisitor
    get() = attributes[WebVisitorKey]
