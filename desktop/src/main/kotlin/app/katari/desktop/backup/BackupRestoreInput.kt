package app.katari.desktop.backup

import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.storage.DocumentUri

/** A restore the user started, as [app.katari.desktop.work.PersistedWork] keeps it until the restore ends. */
internal object BackupRestoreInput {
    private const val KEY_URI = "uri"
    private const val KEY_OPTIONS = "options"
    private const val KEY_PROFILES = "profiles"
    private const val KEY_SYNC = "sync"
    private const val KEY_ROLL_BACK_ACCOUNT = "roll_back_account"

    fun encode(uri: DocumentUri, options: RestoreOptions, sync: Boolean): Map<String, String> = buildMap {
        put(KEY_URI, uri.value)
        put(KEY_OPTIONS, options.asBooleanArray().joinToString(",") { if (it) "1" else "0" })
        // Profile UUIDs never hold a comma.
        options.profileUuids?.let { put(KEY_PROFILES, it.joinToString(",")) }
        put(KEY_SYNC, sync.toString())
        put(KEY_ROLL_BACK_ACCOUNT, options.rollBackAccount.toString())
    }

    fun decode(input: Map<String, String>): Triple<DocumentUri, RestoreOptions, Boolean>? {
        val uri = input[KEY_URI]?.let(::DocumentUri) ?: return null
        val flags = input[KEY_OPTIONS]?.split(',')?.map { it == "1" }?.toBooleanArray() ?: return null
        val profiles = input[KEY_PROFILES]?.split(',')?.filter(String::isNotEmpty)?.toSet()
        return Triple(
            uri,
            RestoreOptions.fromBooleanArray(flags, profiles, input[KEY_ROLL_BACK_ACCOUNT].toBoolean()),
            input[KEY_SYNC].toBoolean(),
        )
    }
}
