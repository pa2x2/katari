package mihon.extension.runtime.jvm.preferences

import android.content.SharedPreferences
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFileStore
import tachiyomi.core.common.preference.DesktopPreferenceStore

/**
 * Each profile's source settings, in [files] by Android's file names, so a backup restored from Android writes the same
 * names. Desktop never had settings from before profiles owned them, so there is no legacy file.
 */
class DesktopSourcePreferenceFileStore(private val files: SharedPreferencesDirectory) : SourcePreferenceFileStore {

    override fun open(name: String): SharedPreferences = files.open(name)

    /** The settings file [name] as the app's own settings are read and written, for backups. */
    fun preferenceStore(name: String): DesktopPreferenceStore = files.open(name).store

    override fun names(): Sequence<String> = files.names()

    override fun delete(name: String) {
        files.delete(name)
    }

    override fun hasLegacyFile(name: String): Boolean = false

    override fun openLegacyFile(name: String): SharedPreferences = error("Desktop has no legacy settings file $name")

    override fun deleteLegacyFile(name: String) = Unit
}
