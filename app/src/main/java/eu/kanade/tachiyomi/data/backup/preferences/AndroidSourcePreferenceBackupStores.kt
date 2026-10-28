package eu.kanade.tachiyomi.data.backup.preferences

import android.content.Context
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.source.EntrySourceSettingsFeature
import mihon.feature.profiles.core.ProfileAwarePreferenceStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.extensionPreferenceSourceId
import mihon.feature.profiles.sourcepreferences.sourcePreferenceFileName
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore

/** Each profile's settings files of the sources, which [SourcePreferenceFiles] owns. */
@Inject
class AndroidSourcePreferenceBackupStores(
    private val context: Context,
    private val sourceSettingsFeature: EntrySourceSettingsFeature,
    private val sourcePreferenceFiles: SourcePreferenceFiles,
) : SourcePreferenceBackupStores {
    override fun sourceKeys(): List<Long> = sourceSettingsFeature.supportedSourceIds()

    override suspend fun read(profileId: Long, sourceKey: Long): SourcePreferenceSnapshot {
        val sourceId = sourcePreferenceFiles.sourceIdOf(sourceKey)
        return SourcePreferenceSnapshot(
            fileName = sourcePreferenceFileName(profileId, sourceId),
            values = sourcePreferenceFiles.open(profileId, sourceId).all
                .filterKeys { !ProfileAwarePreferenceStore.Namespace.isNamespacedKey(it) },
        )
    }

    override suspend fun storeFor(profileId: Long, sourceKey: Long?, fileName: String): PreferenceStore? {
        val sourceId = sourceKey?.let { sourcePreferenceFiles.sourceIdOf(it) }
            ?: backedUpSourceId(fileName)
            ?: return null
        return AndroidPreferenceStore(context, sourcePreferenceFiles.open(profileId, sourceId))
    }

    // Backups without a source ID name the file the source used: Mihon's `source_<id>`, or `source_<profile>_<id>`
    private fun backedUpSourceId(fileName: String): Long? {
        return extensionPreferenceSourceId(fileName)
            ?: PROFILE_SOURCE_FILE.matchEntire(fileName)?.groupValues?.get(1)?.toLongOrNull()
    }

    private companion object {
        val PROFILE_SOURCE_FILE = Regex("""source_-?\d+_(-?\d+)""")
    }
}
