package mihon.extension.runtime.jvm.preferences

import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceSnapshot
import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.backedUpSourcePreferenceSourceId
import mihon.feature.profiles.sourcepreferences.sourcePreferenceFileName
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.source.service.SourceManager

/**
 * Each profile's settings files of the sources that have settings, which [SourcePreferenceFiles] owns, read and written
 * as Android's backups read and write them. Desktop's files hold only the sources' own keys.
 */
class DesktopSourcePreferenceBackupStores(
    private val sourceManager: SourceManager,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
    private val fileStore: DesktopSourcePreferenceFileStore,
) : SourcePreferenceBackupStores {

    override fun sourceKeys(): List<Long> = sourceManager.getAll()
        .filterIsInstance<ConfigurableSource>()
        .map { it.id }
        .sorted()

    override suspend fun read(profileId: Long, sourceKey: Long): SourcePreferenceSnapshot {
        val sourceId = sourcePreferenceFiles.sourceIdOf(sourceKey)
        return SourcePreferenceSnapshot(
            fileName = sourcePreferenceFileName(profileId, sourceId),
            values = sourcePreferenceFiles.open(profileId, sourceId).all,
        )
    }

    override suspend fun storeFor(profileId: Long, sourceKey: Long?, fileName: String): PreferenceStore? {
        val sourceId = sourceKey?.let { sourcePreferenceFiles.sourceIdOf(it) }
            ?: backedUpSourcePreferenceSourceId(fileName)
            ?: return null
        return fileStore.preferenceStore(sourcePreferenceFileName(profileId, sourceId))
    }
}
