package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceBackupStores
import eu.kanade.tachiyomi.data.backup.preferences.SourcePreferenceSnapshot
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Desktop has no extensions yet, so no source keeps settings there: a backup has none to hold, and a restore leaves
 * out the ones a backup from Android holds rather than write them where no source would read them. Stands in until
 * desktop hosts extensions.
 */
@Inject
class NoSourcePreferenceBackupStores : SourcePreferenceBackupStores {
    override fun sourceKeys(): List<Long> = emptyList()

    override suspend fun read(profileId: Long, sourceKey: Long): SourcePreferenceSnapshot =
        SourcePreferenceSnapshot(fileName = "", values = emptyMap<String, Any>())

    override suspend fun storeFor(profileId: Long, sourceKey: Long?, fileName: String): PreferenceStore? = null
}
