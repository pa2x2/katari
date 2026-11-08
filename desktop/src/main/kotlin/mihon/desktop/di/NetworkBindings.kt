package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.tls.LinuxCertificateAuthorities
import eu.kanade.tachiyomi.network.tls.SystemCertificateAuthorities
import eu.kanade.tachiyomi.network.tls.systemTrustManager
import kotlinx.coroutines.CoroutineScope
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.network.NetworkConnectivity
import mihon.desktop.network.DesktopWebViewInterceptorMessages
import mihon.desktop.network.PortalNetworkConnectivity
import mihon.desktop.storage.DesktopDirectories
import mihon.extension.runtime.jvm.application.ExtensionApplication

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
    ): NetworkHelper = NetworkHelper(
        context = application,
        cacheDirectory = directories.cache.toFile(),
        preferences = preferences,
        webViewMessages = DesktopWebViewInterceptorMessages(),
        trustManager = systemTrustManager(certificateAuthorities),
    )
}
