package eu.kanade.tachiyomi.data.backup.preferences

import tachiyomi.core.common.preference.PreferenceStore

/** The settings sources keep for themselves, which each profile has its own of. */
interface SourcePreferenceBackupStores {
    /** The local keys of the sources whose settings can be backed up. */
    fun sourceKeys(): List<Long>

    /** [profileId]'s settings of the source stored under [sourceKey]. */
    suspend fun read(profileId: Long, sourceKey: Long): SourcePreferenceSnapshot

    /**
     * Where [profileId]'s restored settings of a source go: of the source stored under [sourceKey], or, for backups
     * without it, of the source the settings named [fileName] belonged to. Null when they can't be kept here.
     */
    suspend fun storeFor(profileId: Long, sourceKey: Long?, fileName: String): PreferenceStore?
}

/** A source's settings by key, and the name a backup records them under. */
class SourcePreferenceSnapshot(
    val fileName: String,
    val values: Map<String, *>,
)
