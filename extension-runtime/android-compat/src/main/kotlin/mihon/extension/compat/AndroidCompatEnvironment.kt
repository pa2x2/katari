package mihon.extension.compat

import xyz.nulldev.androidcompat.androidimpl.CustomContext
import java.io.File

/**
 * What the extension host gives the Android stand-ins: the emulated app's folders, its shared preferences, its
 * identity and where toasts go. The host [install]s it once at start, before any extension or stand-in class runs;
 * stand-ins that read it in their static initialisers (`Environment`, `BuildConfigCompat`) fail if it is missing.
 *
 * @property dataDir the app's private folder, Android's `/data/data/<package>`; files, databases and the emulated
 * external storage are laid out under it.
 * @property showToast receives the text of every toast an extension shows, on the extension's thread.
 */
class AndroidCompatEnvironment(
    val dataDir: File,
    val cacheDir: File,
    val sharedPreferences: SharedPreferencesFiles,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val showToast: (CharSequence) -> Unit,
) {
    /** The base context of the emulated app, which `Application.attach` and every `Service` take. */
    val context: CustomContext by lazy { CustomContext(this) }

    companion object {
        @Volatile
        private var installed: AndroidCompatEnvironment? = null

        @JvmStatic
        fun install(environment: AndroidCompatEnvironment) {
            synchronized(this) {
                check(installed == null) { "The Android environment is already installed" }
                installed = environment
            }
        }

        @JvmStatic
        fun current(): AndroidCompatEnvironment =
            checkNotNull(installed) { "The extension host has not installed the Android environment yet" }
    }
}
