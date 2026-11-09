package app.katari.telemetry

import com.posthog.kmp.ErrorTrackingConfig
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogBeforeSend
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import kotlinx.coroutines.CoroutineScope

internal actual object PostHogClient {
    actual fun setUp(
        host: TelemetryHost,
        addedProperties: (event: String) -> Map<String, Any>?,
        scope: CoroutineScope,
    ) {
        val session = LinuxSession.read().properties
        val beforeSend = PostHogBeforeSend { event ->
            addedProperties(event.event)?.let { added -> event.copy(properties = event.properties + added + session) }
        }
        val config = PostHogConfig(
            projectToken = PostHogProject.TOKEN,
            host = PostHogProject.HOST,
            preloadFeatureFlags = false,
            beforeSend = listOf(beforeSend),
            errorTracking = ErrorTrackingConfig(autoCapture = true),
        )
        PostHog.setup(config, PostHogContext())
        // PostHog keeps an opt-out from an earlier run, while the user's switches are what decide
        if (PostHog.isOptedOut()) PostHog.optIn()
        PostHog.register(APP_VERSION, host.appVersion)
        // The JVM library reports no app lifecycle, so the desktop app reports its start under Android's name for it
        PostHog.capture(APPLICATION_OPENED)
    }

    actual fun optIn() {
        PostHog.optIn()
    }

    actual fun optOut() {
        PostHog.optOut()
    }

    actual fun captureException(throwable: Throwable) {
        PostHog.captureException(throwable)
    }
}

private const val APP_VERSION = "\$app_version"
private const val APPLICATION_OPENED = "Application Opened"
