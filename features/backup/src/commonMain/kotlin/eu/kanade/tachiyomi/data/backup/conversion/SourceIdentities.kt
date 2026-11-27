package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.identity.SourceIdentity

/** The source with the identity its id has among Tachiyomi's extensions, which other apps' manga sources come from. */
internal fun BackupSource.withTachiyomiIdentity(): BackupSource {
    val identity = SourceIdentity.ofLegacyKey(sourceId)
    return copy(idScheme = identity.scheme.key, externalId = identity.externalId)
}
