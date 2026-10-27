package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Moves the settings a source kept in its one shared file, from before each profile had its own, into every profile.
 *
 * Until then a legacy extension read [shared] in every profile. The older profile migration copied that file into the
 * default profile's and deleted it, so the extension started over from defaults, and the default profile's file still
 * holds what that took away. So every profile gets the default profile's file with [shared] on top, as each profile saw
 * the same file, and keeps its own keys where they don't conflict, such as values a restore wrote there. Writing it
 * again gives the same result, so a run that was cut short is simply redone.
 */
internal fun copySharedSourceSettings(
    shared: Map<String, *>,
    defaultProfileFile: SharedPreferences,
    profileFiles: Collection<SharedPreferences>,
) {
    val settings = defaultProfileFile.all + shared
    profileFiles.forEach { file ->
        file.edit(commit = true) {
            settings.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }
    }
}
