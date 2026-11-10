package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.desktop.network.DesktopWebViewInterceptorMessages
import app.katari.desktop.network.PortalNetworkConnectivity
import app.katari.desktop.storage.DesktopDirectories
import app.katari.extension.runtime.jvm.application.ExtensionApplication
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.network.tls.LinuxCertificateAuthorities
import eu.kanade.tachiyomi.network.tls.SystemCertificateAuthorities
import eu.kanade.tachiyomi.network.tls.systemTrustManager
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetupState
import kotlinx.coroutines.CoroutineScope

@BindingContainer
object NetworkBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun systemCertificateAuthorities(operatingSystem: DesktopOperatingSystem): SystemCertificateAuthorities =
        when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> LinuxCertificateAuthorities()
        }

    @Provides
    @SingleIn(AppScope::class)
    fun networkConnectivity(
        operatingSystem: DesktopOperatingSystem,
        @AppCoroutineScope scope: CoroutineScope,
    ): NetworkConnectivity = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> PortalNetworkConnectivity(scope)
    }

    // The interceptors' toasts go through the Android stand-ins, as extensions' own do
    @Provides
    @SingleIn(AppScope::class)
    fun networkHelper(
        application: ExtensionApplication,
        directories: DesktopDirectories,
        preferences: NetworkPreferences,
        certificateAuthorities: SystemCertificateAuthorities,
        webViewMessages: WebViewInterceptorMessages,
    ): NetworkHelper = NetworkHelper(
        context = application,
        cacheDirectory = directories.cache.toFile(),
        preferences = preferences,
        webViewMessages = webViewMessages,
        trustManager = systemTrustManager(certificateAuthorities),
    )

    // The set-up is asked as a message is worded: it downloads with the client, so it is built after it
    @Provides
    @SingleIn(AppScope::class)
    fun webViewInterceptorMessages(webViewSetup: Lazy<WebViewSetup>): WebViewInterceptorMessages =
        DesktopWebViewInterceptorMessages { webViewSetup.value.state.value != WebViewSetupState.Ready }
}
