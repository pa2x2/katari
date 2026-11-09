package app.katari.telemetry

import android.os.Build
import app.katari.telemetry.freeze.ApplicationNotRespondingReports
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import kotlinx.coroutines.CoroutineScope

internal actual object PostHogClient {
    actual fun start(host: TelemetryHost, consent: TelemetryConsent, scope: CoroutineScope) {
        val application = host.application
        // Forks and Katari's other builds, such as the development and preview ones, would mix into the release's data
        if (application.packageName != KATARI_PACKAGE) return

        val config = PostHogAndroidConfig(PostHogProject.TOKEN, PostHogProject.HOST).apply {
            // Screen views name activities, which say nothing in an app of one activity, and a deep link can carry
            // the address of what someone reads. Push notifications and feature flags aren't used.
            captureScreenViews = false
            captureDeepLinks = false
            capturePushNotificationSubscriptions = false
            capturePushNotificationOpened = false
            preloadFeatureFlags = false

            errorTrackingConfig.autoCapture = true
            errorTrackingConfig.captureNativeCrashes = true
            addBeforeSend { event -> event.takeIf { consent.allows(it.event) } }
        }
        PostHogAndroid.setup(application, config)
        PostHog.register("preferred_abi", Build.SUPPORTED_ABIS[0])
        ApplicationNotRespondingReports(application).reportNew(scope)
    }

    actual fun captureException(throwable: Throwable) {
        PostHog.captureException(throwable)
    }
}

private const val KATARI_PACKAGE = "app.katari"
