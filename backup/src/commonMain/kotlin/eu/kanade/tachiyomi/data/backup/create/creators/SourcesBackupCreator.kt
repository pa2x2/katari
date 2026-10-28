package eu.kanade.tachiyomi.data.backup.create.creators

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.service.SourceManager

@Inject
class SourcesBackupCreator(
    private val sourceManager: SourceManager,
    private val identities: SourceIdentityRepository,
) {

    suspend operator fun invoke(sourceKeys: Set<Long>): List<BackupSource> {
        return sourceKeys.map { key ->
            val registered = identities.find(key)
            val identity = registered?.identity ?: SourceIdentity.ofLegacyKey(key)
            BackupSource(
                name = sourceManager.getOrStub(key).name,
                sourceId = key,
                idScheme = identity.scheme.key,
                externalId = identity.externalId,
                providerKind = registered?.provider?.kind?.key,
            )
        }
    }
}
