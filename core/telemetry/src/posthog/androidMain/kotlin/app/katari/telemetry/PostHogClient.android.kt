package app.katari.telemetry

import android.os.Build
import app.katari.telemetry.freeze.ApplicationNotRespondingReports
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import kotlinx.coroutines.CoroutineScope

internal actual object PostHogClient {
    actual fun setUp(
        token: String,
        host: TelemetryHost,
        addedProperties: (event: String) -> Map<String, Any>?,
        onActivityResumed: (name: String) -> Unit,
        scope: CoroutineScope,
    ) {
        val application = host.application
        val resumedActivity = ResumedActivity(onActivityResumed)
        application.registerActivityLifecycleCallbacks(resumedActivity)

        val config = PostHogAndroidConfig(token, PostHogProject.HOST).apply {
            // Screen views name activities, which say nothing in an app of one activity, and a deep link can carry
            // the address of what someone reads. The day's report counts the days the app is used, in place of an
            // event each time it opens or goes to the background. Push notifications and feature flags aren't used.
            captureScreenViews = false
            captureApplicationLifecycleEvents = false
            captureDeepLinks = false
            capturePushNotificationSubscriptions = false
            capturePushNotificationOpened = false
            preloadFeatureFlags = false

            errorTrackingConfig.autoCapture = true
            errorTrackingConfig.captureNativeCrashes = true
            addBeforeSend { event ->
                addedProperties(event.event)?.let { added ->
                    val properties = added + resumedActivity.properties + event.properties.orEmpty()
                    event.copy(properties = properties.toMutableMap())
                }
            }
        }
        PostHogAndroid.setup(application, config)
        // PostHog keeps an opt-out from an earlier run, while the user's switches are what decide
        if (PostHog.isOptOut()) PostHog.optIn()
        PostHog.register("preferred_abi", Build.SUPPORTED_ABIS[0])
        ApplicationNotRespondingReports(application).reportNew(scope)
    }

    actual fun optIn() {
        PostHog.optIn()
    }

    actual fun optOut() {
        PostHog.optOut()
    }

    actual fun capture(event: String, properties: Map<String, Any>) {
        PostHog.capture(event, properties = properties)
    }

    actual fun captureException(throwable: Throwable) {
        PostHog.captureException(throwable)
    }
}
