package app.katari.sync.client.kind.extension

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * An extension repository the person added. Everything but the index URL comes from the repository's index, which
 * each device fetches again; it travels so the repository shows before that, signing key included.
 */
@Serializable
data class ExtensionRepositoryPayload(
    @ProtoNumber(1) val indexUrl: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val badgeLabel: String,
    @ProtoNumber(4) val signingKey: String? = null,
    @ProtoNumber(5) val contactWebsite: String,
    @ProtoNumber(6) val contactDiscord: String? = null,
    @ProtoNumber(7) val format: String,
    @ProtoNumber(8) val extensionListUrl: String? = null,
)
