package mihon.feature.profiles.sourcepreferences

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dev.zacsweers.metro.Inject
import java.io.File

/**
 * Android's shared-preferences files. The App sends the legacy files' names to [SourcePreferenceFiles.forSource]; its
 * base context still opens the files themselves.
 */
@Inject
class AndroidSourcePreferenceFileStore(private val application: Application) : SourcePreferenceFileStore {

    override fun open(name: String): SharedPreferences = application.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun names(): Sequence<String> {
        return sharedPreferencesDir.listFiles()
            ?.asSequence()
            ?.filter { it.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            .orEmpty()
    }

    override fun delete(name: String) {
        application.deleteSharedPreferences(name)
    }

    override fun hasLegacyFile(name: String): Boolean =
        File(sharedPreferencesDir, "$name.xml").exists() || File(sharedPreferencesDir, "$name.xml.bak").exists()

    override fun openLegacyFile(name: String): SharedPreferences =
        application.baseContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun deleteLegacyFile(name: String) {
        application.baseContext.deleteSharedPreferences(name)
    }

    private val sharedPreferencesDir: File
        get() = File(application.applicationInfo.dataDir, "shared_prefs")
}
