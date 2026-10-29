package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import mihon.desktop.network.DesktopWebViewInterceptorMessages
import mihon.desktop.storage.DesktopDirectories
import mihon.extension.runtime.jvm.application.ExtensionApplication

@BindingContainer
object NetworkBindings {
    // The interceptors' toasts go through the Android stand-ins, as extensions' own do
    @Provides
    @SingleIn(AppScope::class)
    fun networkHelper(
        application: ExtensionApplication,
        directories: DesktopDirectories,
        preferences: NetworkPreferences,
    ): NetworkHelper = NetworkHelper(
        context = application,
        cacheDirectory = directories.cache.toFile(),
        preferences = preferences,
        webViewMessages = DesktopWebViewInterceptorMessages(),
    )
}
