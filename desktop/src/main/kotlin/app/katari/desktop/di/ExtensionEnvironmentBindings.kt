package app.katari.desktop.di

import app.katari.core.common.network.cookie.AppCookieStore
import app.katari.core.common.web.ExtensionPages
import app.katari.desktop.extension.dialog.WindowExtensionDialogs
import app.katari.desktop.storage.DesktopDirectories
import app.katari.domain.app.AppIdentity
import app.katari.extension.compat.AndroidCompatEnvironment
import app.katari.extension.runtime.jvm.application.ExtensionApplication
import app.katari.extension.runtime.jvm.application.MainLooperThread
import app.katari.extension.runtime.jvm.application.WindowActivity
import app.katari.extension.runtime.jvm.cookie.DesktopWebViewCookies
import app.katari.extension.runtime.jvm.preferences.SharedPreferencesDirectory
import app.katari.presentation.kit.feedback.AppMessages
import app.katari.presentation.kit.theme.SystemColorScheme
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.BuildConfig

/**
 * The Android app extensions run in on desktop: the stand-ins' environment, laid out under the app's data and cache
 * folders, on the app's cookie store, on the app's pages for extensions' web views and on its window for their dialogs,
 * the `Application` they're handed and the `Activity` that stands for the window. Extensions' toasts become app
 * messages, and their night mode is the desktop's dark preference, which is asked for only once an extension reads it.
 */
@BindingContainer
object ExtensionEnvironmentBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun androidCompatEnvironment(
        directories: DesktopDirectories,
        identity: AppIdentity,
        messages: AppMessages,
        colorScheme: Lazy<SystemColorScheme>,
        webPages: ExtensionPages,
        dialogs: WindowExtensionDialogs,
    ): AndroidCompatEnvironment {
        val dataDir = directories.data.resolve("extension-host")
        return AndroidCompatEnvironment(
            dataDir = dataDir.toFile(),
            cacheDir = directories.cache.resolve("extension-host").toFile(),
            sharedPreferences = SharedPreferencesDirectory(dataDir.resolve("shared_prefs"), MainLooperThread),
            cookies = DesktopWebViewCookies(AppCookieStore.store),
            webPages = webPages,
            dialogs = dialogs,
            packageName = identity.applicationId,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            showToast = { text -> messages.show(text.toString()) },
            isNightMode = { colorScheme.value.prefersDark.value },
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun extensionApplication(
        environment: AndroidCompatEnvironment,
        sourcePreferenceFiles: () -> SourcePreferenceFiles,
    ): ExtensionApplication = ExtensionApplication(environment, sourcePreferenceFiles)

    @Provides
    @SingleIn(AppScope::class)
    fun windowActivity(application: ExtensionApplication): WindowActivity = WindowActivity(application)
}
