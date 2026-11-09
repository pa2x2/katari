package app.katari.telemetry

import com.posthog.kmp.ErrorTrackingConfig
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogBeforeSend
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import kotlinx.coroutines.CoroutineScope

internal actual object PostHogClient {
    actual fun start(host: TelemetryHost, consent: TelemetryConsent, scope: CoroutineScope) {
        val config = PostHogConfig(
            projectToken = PostHogProject.TOKEN,
            host = PostHogProject.HOST,
            preloadFeatureFlags = false,
            beforeSend = listOf(PostHogBeforeSend { event -> event.takeIf { consent.allows(it.event) } }),
            errorTracking = ErrorTrackingConfig(autoCapture = true),
        )
        PostHog.setup(config, PostHogContext())
        PostHog.register(APP_VERSION, host.appVersion)
        // The JVM library reports no app lifecycle, so the desktop app reports its start under Android's name for it
        PostHog.capture(APPLICATION_OPENED)
    }

    actual fun captureException(throwable: Throwable) {
        PostHog.captureException(throwable)
    }
}

private const val APP_VERSION = "\$app_version"
private const val APPLICATION_OPENED = "Application Opened"
