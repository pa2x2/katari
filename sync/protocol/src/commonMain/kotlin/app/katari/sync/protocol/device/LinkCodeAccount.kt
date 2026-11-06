package app.katari.sync.protocol.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Asks which account a link code joins, so a new device can name it before joining (D027). Looking a code up doesn't
 * use it, and counts toward the same guessing limit as joining with it; a code that is wrong, expired or used gets the
 * same refusal.
 */
@Serializable
data class LinkCodeAccountRequest(
    /** As the person typed it; case, dashes and spaces don't matter. */
    @ProtoNumber(1) val code: String,
)

/** The account a link code joins, by the name it signs in with, and nothing else about it. */
@Serializable
data class LinkCodeAccount(
    @ProtoNumber(1) val accountName: String,
)
