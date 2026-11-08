package app.katari.sync.server.http

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.version.ProtocolCompatibility
import app.katari.sync.protocol.version.ProtocolVersionRange
import app.katari.sync.protocol.version.SyncProtocolVersion
import io.ktor.server.application.createRouteScopedPlugin

/** The protocol versions this server speaks; raising the oldest drops apps that have not updated. */
internal val SupportedProtocolVersions = ProtocolVersionRange(oldest = 1, newest = SyncProtocolVersion.CURRENT)

/** Refuses calls from an app whose protocol version this server does not speak, or that sends none. */
internal val ProtocolVersionCheck = createRouteScopedPlugin("ProtocolVersionCheck") {
    onCall { call ->
        val version = call.request.headers[SyncHttp.PROTOCOL_VERSION_HEADER]?.trim()?.toIntOrNull()
        if (version == null || SupportedProtocolVersions.compatibilityOf(version) != ProtocolCompatibility.SUPPORTED) {
            call.respondRefusal(
                SyncErrorCode.PROTOCOL_VERSION_UNSUPPORTED,
                message = "Protocol version ${version ?: "missing"}",
                supportedVersions = SupportedProtocolVersions,
            )
        }
    }
}
