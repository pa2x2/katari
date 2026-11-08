package app.katari.sync.client.kind.merge

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * One entry's place in a merge the person made: the group's target entry (the target is a member of its own group)
 * and its position there. An entry belongs to one group at most, so moving it to another group changes [target].
 */
@Serializable
data class MergeMemberPayload(
    @ProtoNumber(1) val memberKey: String,
    @ProtoNumber(2) val target: Stamped<String>,
    @ProtoNumber(3) val position: Stamped<Long>,
    @ProtoNumber(4) val removed: Stamped<Boolean>,
)
