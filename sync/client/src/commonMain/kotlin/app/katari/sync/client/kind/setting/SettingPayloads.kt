package app.katari.sync.client.kind.setting

import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.setting.value.SettingValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** One person-level preference of a profile. */
@Serializable
data class ProfileSettingPayload(
    @ProtoNumber(1) val profileUuid: String,
    /** The key as preference classes ask for it, with a source key in it as an index into [keySources]. */
    @ProtoNumber(2) val key: String,
    @ProtoNumber(3) val keySources: List<SourceReference> = emptyList(),
    @ProtoNumber(4) val value: Stamped<SettingValue>,
)

/** One person-level preference of the store all profiles share, the same for the whole account. */
@Serializable
data class SharedSettingPayload(
    /** The key as preference classes ask for it, with a source key in it as an index into [keySources]. */
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val keySources: List<SourceReference> = emptyList(),
    @ProtoNumber(3) val value: Stamped<SettingValue>,
)

/**
 * One setting a source keeps for a profile, by the source's own ID, which names its settings file on every device.
 * The source's keys and values are its own business and travel as they are.
 */
@Serializable
data class SourceSettingPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val sourceId: Long,
    @ProtoNumber(3) val key: String,
    @ProtoNumber(4) val value: Stamped<SettingValue>,
)
