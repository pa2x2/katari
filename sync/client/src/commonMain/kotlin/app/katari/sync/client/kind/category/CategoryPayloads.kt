package app.katari.sync.client.kind.category

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class CategoryPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val name: Stamped<String>,
    @ProtoNumber(3) val order: Stamped<Long>,
    /** Per-category sort and display flags. */
    @ProtoNumber(4) val flags: Stamped<Long>,
)

/** One entry in one category. Removing it keeps the record with [removed] set, so adding and removing both merge. */
@Serializable
data class CategoryMembershipPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val categoryKey: String,
    @ProtoNumber(3) val removed: Stamped<Boolean>,
)

/** A category's library update rules, the Default category's included. */
@Serializable
data class CategoryUpdateRulesPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val categoryKey: String,
    @ProtoNumber(3) val autoUpdate: Stamped<Boolean>,
    @ProtoNumber(4) val customRules: Stamped<Boolean>,
    @ProtoNumber(5) val skipCompleted: Stamped<Boolean>,
    @ProtoNumber(6) val skipUnseen: Stamped<Boolean>,
    @ProtoNumber(7) val skipNotStarted: Stamped<Boolean>,
    @ProtoNumber(8) val skipOutsideReleasePeriod: Stamped<Boolean>,
    @ProtoNumber(9) val intervalHours: Stamped<Long?>,
)
