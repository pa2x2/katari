package app.katari.sync.client.kind.profile

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** A profile. Its lock lives in the profile's settings, which sync as settings. */
@Serializable
data class ProfilePayload(
    /** `default` for the profile every install starts with, which pairs by itself. */
    @ProtoNumber(1) val uuid: String,
    @ProtoNumber(2) val name: Stamped<String>,
    @ProtoNumber(3) val colorSeed: Stamped<Long>,
    @ProtoNumber(4) val position: Stamped<Long>,
    @ProtoNumber(5) val archived: Stamped<Boolean>,
)
