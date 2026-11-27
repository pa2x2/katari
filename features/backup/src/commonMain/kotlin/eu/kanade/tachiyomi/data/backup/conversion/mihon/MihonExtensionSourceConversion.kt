package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore

/**
 * A store message has fields a repo message lacks, or an index file's URL where a repo has its folder's. Both read as
 * [MihonExtensionSource], so telling them apart takes their content (research/04: field 106 holds either).
 */
internal val MihonExtensionSource.isStore: Boolean
    get() = isLegacy != null || contactDiscord != null || extensionListUrl != null ||
        url.substringAfterLast('/').contains('.')

/** The store as Katari backs it up; null for a repo, which points at an index stores no longer serve. */
internal fun MihonExtensionSource.toBackupExtensionStore(): BackupExtensionStore? {
    if (!isStore) return null
    return BackupExtensionStore(
        indexUrl = url,
        name = name,
        badgeLabel = label,
        signingKey = signingKey,
        contactWebsite = website,
        contactDiscord = contactDiscord,
        isLegacy = isLegacy,
        extensionListUrl = extensionListUrl,
    )
}
