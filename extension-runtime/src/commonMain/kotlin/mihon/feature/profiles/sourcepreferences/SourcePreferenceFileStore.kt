package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences

/**
 * The platform's settings files by name, where [SourcePreferenceFiles] keeps each profile's source settings. A legacy
 * file is a source's one file from before profiles owned them, which extensions open by the same name the app now
 * routes to the profiles' files, so it's reached past that routing.
 */
interface SourcePreferenceFileStore {

    /** The settings file [name], created on its first write. */
    fun open(name: String): SharedPreferences

    /** The names of every settings file the app has, its own as well as the sources'. */
    fun names(): Sequence<String>

    fun delete(name: String)

    fun hasLegacyFile(name: String): Boolean

    fun openLegacyFile(name: String): SharedPreferences

    fun deleteLegacyFile(name: String)
}
