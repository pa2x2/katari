package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat

@Serializable
class BackupExtensionStore(
    @ProtoNumber(1) var indexUrl: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var badgeLabel: String?,
    @ProtoNumber(5) var signingKey: String?,
    @ProtoNumber(4) var contactWebsite: String,
    @ProtoNumber(6) var contactDiscord: String?,
    // Written for app versions before formats, which read only this
    @ProtoNumber(7) var isLegacy: Boolean?,
    @ProtoNumber(8) var extensionListUrl: String?,
    // Absent in backups from before formats, whose isLegacy says it
    @ProtoNumber(9) var format: String? = null,
)

val backupExtensionStoreMapper = { store: ExtensionStore ->
    BackupExtensionStore(
        indexUrl = store.indexUrl,
        name = store.name,
        badgeLabel = store.badgeLabel,
        signingKey = store.signingKey,
        contactWebsite = store.contact.website,
        contactDiscord = store.contact.discord,
        isLegacy = store.format == ExtensionStoreFormat.LEGACY,
        extensionListUrl = store.extensionListUrl,
        format = store.format.key,
    )
}
