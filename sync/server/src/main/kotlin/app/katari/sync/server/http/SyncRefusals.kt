package app.katari.sync.server.http

import app.katari.sync.protocol.error.SyncError
import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.version.ProtocolVersionRange
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlin.time.Duration

internal suspend fun ApplicationCall.respondRefusal(
    code: SyncErrorCode,
    message: String = "",
    supportedVersions: ProtocolVersionRange? = null,
) {
    if (code == SyncErrorCode.UNAUTHORIZED) response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer")
    respondProto(SyncError(code, message, supportedVersions), code.httpStatus)
}

/** A sign-in refused unchecked after too many failures. */
internal suspend fun ApplicationCall.respondThrottled(retryAfter: Duration) {
    response.headers.append(HttpHeaders.RetryAfter, retryAfter.inWholeSeconds.plus(1).toString())
    respondProto(
        SyncError(
            code = SyncErrorCode.UNAUTHORIZED,
            message = "Too many failed attempts",
            retryAfterMillis = retryAfter.inWholeMilliseconds,
        ),
        HttpStatusCode.TooManyRequests,
    )
}

private val SyncErrorCode.httpStatus: HttpStatusCode
    get() = when (this) {
        SyncErrorCode.UNAUTHORIZED -> HttpStatusCode.Unauthorized
        SyncErrorCode.DEVICE_REVOKED -> HttpStatusCode.Forbidden
        SyncErrorCode.CURSOR_EXPIRED -> HttpStatusCode.Gone
        SyncErrorCode.CURSOR_UNKNOWN -> HttpStatusCode.Conflict
        SyncErrorCode.PROTOCOL_VERSION_UNSUPPORTED -> HttpStatusCode.BadRequest
    }
