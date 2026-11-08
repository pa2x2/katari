package app.katari.sync.server.web.request

import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.util.AttributeKey
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

/** The web page's forms are a few short fields; anything longer is not one of them. */
private const val MAX_FORM_BYTES = 16L * 1024

internal val FormKey = AttributeKey<Parameters>("WebForm")

/** The POSTed form, read once by [webRequests] to check its token; empty when the body is not a URL-encoded form. */
internal val ApplicationCall.form: Parameters
    get() = attributes[FormKey]

internal suspend fun ApplicationCall.readForm(): Parameters {
    if (!request.contentType().match(ContentType.Application.FormUrlEncoded)) return Parameters.Empty
    val bytes = receiveChannel().readBuffer(MAX_FORM_BYTES + 1).readByteArray()
    if (bytes.size > MAX_FORM_BYTES) throw PayloadTooLargeException(MAX_FORM_BYTES)
    return parseQueryString(bytes.decodeToString())
}
