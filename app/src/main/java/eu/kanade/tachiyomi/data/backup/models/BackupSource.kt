package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity

@Serializable
data class BackupSource(
    @ProtoNumber(1) var name: String = "",
    /** The local key on the device the backup was made on, which the rest of the backup refers to the source by. */
    @ProtoNumber(2) var sourceId: Long,
    // Null in backups made before identities were recorded, and in Mihon's
    @ProtoNumber(3) var idScheme: String? = null,
    @ProtoNumber(4) var externalId: Long? = null,
    @ProtoNumber(5) var providerKind: String? = null,
) {
    val identity: SourceIdentity
        get() {
            val scheme = idScheme ?: return SourceIdentity.ofLegacyKey(sourceId)
            return SourceIdentity(SourceIdScheme(scheme), externalId ?: sourceId)
        }
}
