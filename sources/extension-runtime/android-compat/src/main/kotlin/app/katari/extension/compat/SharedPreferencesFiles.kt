package app.katari.extension.compat

import android.content.SharedPreferences

/**
 * The emulated app's shared preferences files, by the names extensions pass to `Context.getSharedPreferences`. The
 * stand-in context keeps one instance per name, as Android does, so [open] is called once per name until [delete].
 */
interface SharedPreferencesFiles {
    fun open(name: String): SharedPreferences

    /** Removes the file's values and the file itself; false when nothing could be deleted, as on Android. */
    fun delete(name: String): Boolean
}
