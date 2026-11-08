package eu.kanade.tachiyomi.data.backup.preferences

import android.content.Context
import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import app.katari.profiles.core.ProfileAwarePreferenceStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import app.katari.profiles.sourcepreferences.backedUpSourcePreferenceSourceId
import app.katari.profiles.sourcepreferences.sourcePreferenceFileName
import dev.zacsweers.metro.Inject
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
            ?: backedUpSourcePreferenceSourceId(fileName)
            ?: return null
        return AndroidPreferenceStore(context, sourcePreferenceFiles.open(profileId, sourceId))
    }
}
