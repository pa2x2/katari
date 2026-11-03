package mihon.desktop.backup

import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.storage.DocumentUri

/** A backup the user started, as [mihon.desktop.work.PersistedWork] keeps it until the backup ends. */
internal object BackupCreateInput {
    private const val KEY_URI = "uri"
    private const val KEY_OPTIONS = "options"
    private const val KEY_PROFILES = "profiles"

    fun encode(uri: DocumentUri, options: BackupOptions): Map<String, String> = buildMap {
        put(KEY_URI, uri.value)
        put(KEY_OPTIONS, options.asBooleanArray().joinToString(",") { if (it) "1" else "0" })
        options.profileIds?.let { put(KEY_PROFILES, it.joinToString(",")) }
    }

    fun decode(input: Map<String, String>): Pair<DocumentUri, BackupOptions>? {
        val uri = input[KEY_URI]?.let(::DocumentUri) ?: return null
        val flags = input[KEY_OPTIONS]?.split(',')?.map { it == "1" }?.toBooleanArray() ?: return null
        val profiles = input[KEY_PROFILES]?.split(',')?.mapNotNull(String::toLongOrNull)?.toSet()
        return uri to BackupOptions.fromBooleanArray(flags, profiles)
    }
}
