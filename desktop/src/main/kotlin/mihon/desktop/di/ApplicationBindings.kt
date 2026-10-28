package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.PlatformContext
import mihon.desktop.application.ApplicationPlatformContext
import mihon.desktop.application.DesktopBuildInfo
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.domain.app.AppIdentity
import tachiyomi.core.common.util.system.logcat

@BindingContainer
interface ApplicationBindings {
    @Binds
    val ApplicationPlatformContext.platformContext: PlatformContext

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun operatingSystem(): DesktopOperatingSystem = DesktopOperatingSystem.current()

        @Provides
        fun appIdentity(operatingSystem: DesktopOperatingSystem): AppIdentity = AppIdentity(
            applicationId = "app.katari.desktop",
            versionName = DesktopBuildInfo.VERSION_NAME,
            commitSha = DesktopBuildInfo.COMMIT_SHA,
            platformName = operatingSystem.displayName,
        )

        // As on Android, work here outlives whatever started it; its failures are logged instead of reaching the thread
        // that ran them.
        @Provides
        @SingleIn(AppScope::class)
        @AppCoroutineScope
        fun appCoroutineScope(): CoroutineScope {
            val handler = CoroutineExceptionHandler { _, throwable ->
                logcat(LogPriority.ERROR, throwable) { "Uncaught exception in the app scope" }
            }
            return CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
        }

        // Configured as on Android, so shared code reads the same JSON the same way.
        @Provides
        @SingleIn(AppScope::class)
        fun json(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        // As on Android, so backups are encoded the same way.
        @OptIn(ExperimentalSerializationApi::class)
        @Provides
        @SingleIn(AppScope::class)
        fun protoBuf(): ProtoBuf = ProtoBuf
    }
}
