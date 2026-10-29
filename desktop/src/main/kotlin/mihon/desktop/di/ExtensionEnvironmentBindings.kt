package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.BuildConfig
import mihon.desktop.storage.DesktopDirectories
import mihon.domain.app.AppIdentity
import mihon.extension.compat.AndroidCompatEnvironment
import mihon.extension.runtime.jvm.application.ExtensionApplication
import mihon.extension.runtime.jvm.application.MainLooperThread
import mihon.extension.runtime.jvm.preferences.SharedPreferencesDirectory
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.presentation.kit.feedback.AppMessages

/**
 * The Android app extensions run in on desktop: the stand-ins' environment, laid out under the app's data and cache
 * folders, and the `Application` they're handed. Extensions' toasts become app messages.
 */
@BindingContainer
object ExtensionEnvironmentBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun androidCompatEnvironment(
        directories: DesktopDirectories,
        identity: AppIdentity,
        messages: AppMessages,
    ): AndroidCompatEnvironment {
        val dataDir = directories.data.resolve("extension-host")
        return AndroidCompatEnvironment(
            dataDir = dataDir.toFile(),
            cacheDir = directories.cache.resolve("extension-host").toFile(),
            sharedPreferences = SharedPreferencesDirectory(dataDir.resolve("shared_prefs"), MainLooperThread),
            packageName = identity.applicationId,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            showToast = { text -> messages.show(text.toString()) },
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun extensionApplication(
        environment: AndroidCompatEnvironment,
        sourcePreferenceFiles: () -> SourcePreferenceFiles,
    ): ExtensionApplication = ExtensionApplication(environment, sourcePreferenceFiles)
}
