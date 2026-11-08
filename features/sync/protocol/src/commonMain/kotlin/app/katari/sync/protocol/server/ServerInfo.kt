package app.katari.sync.protocol.server

import app.katari.sync.protocol.version.ProtocolVersionRange
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * What a server says about itself before anyone signs in, so an app can tell from the address alone whether it found
 * a sync server and whether the two can talk.
 */
@Serializable
data class ServerInfo(
    @ProtoNumber(1) val instanceId: String,
    @ProtoNumber(2) val protocolVersions: ProtocolVersionRange,
    /**
     * True while the server has no admin account yet. Nobody can sign in until it is created on the server's own web
     * page, at the same address, with the setup code from the server's log, so the app sends the person there.
     */
    @ProtoNumber(3) val needsSetup: Boolean = false,
)
