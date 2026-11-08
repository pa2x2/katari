package mihon.extension.runtime.jvm.application

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import mihon.extension.compat.AndroidCompatEnvironment
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.extensionPreferenceSourceId

/**
 * The `Application` extensions are handed through Injekt, on the stand-ins' context of [environment]: the emulated
 * app's folders, package and own settings files. As Katari's Android `App` does, it sends the settings files extensions
 * open by a source's name without a profile to [sourcePreferenceFiles], which `SourcePreferenceFiles` explains; every
 * other name opens the emulated app's file of that name.
 *
 * @param sourcePreferenceFiles asked for on each source's first open, since the graph that makes them is built later.
 */
class ExtensionApplication(
    environment: AndroidCompatEnvironment,
    private val sourcePreferenceFiles: () -> SourcePreferenceFiles,
) : Application() {

    init {
        attach(environment.context)
    }

    // Android's application context is the application itself, so names opened through it are routed too
    override fun getApplicationContext(): Context = this

    override fun getMainLooper(): Looper = Looper.getMainLooper()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val sourceId = name?.let(::extensionPreferenceSourceId) ?: return super.getSharedPreferences(name, mode)
        return sourcePreferenceFiles().forSource(sourceId)
    }
}
