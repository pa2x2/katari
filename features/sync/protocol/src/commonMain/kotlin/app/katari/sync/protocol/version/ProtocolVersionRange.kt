package app.katari.sync.protocol.version

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** The protocol versions a server accepts, both ends included. */
@Serializable
data class ProtocolVersionRange(
    @ProtoNumber(1) val oldest: Int,
    @ProtoNumber(2) val newest: Int,
) {
    fun compatibilityOf(appVersion: Int): ProtocolCompatibility = when {
        appVersion < oldest -> ProtocolCompatibility.APP_TOO_OLD
        appVersion > newest -> ProtocolCompatibility.SERVER_TOO_OLD
        else -> ProtocolCompatibility.SUPPORTED
    }
}

/** Which side has to be updated before an app and a server can sync. */
enum class ProtocolCompatibility {
    SUPPORTED,
    APP_TOO_OLD,
    SERVER_TOO_OLD,
}
