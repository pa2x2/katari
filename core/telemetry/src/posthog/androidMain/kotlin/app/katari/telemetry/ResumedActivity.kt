package app.katari.telemetry

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlin.concurrent.Volatile

/**
 * The activity in front, which every event carries beside the screen the app's navigation shows: the readers, the
 * player and the settings run in activities of their own, outside that navigation.
 */
internal class ResumedActivity : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var name: String? = null

    val properties: Map<String, Any>
        get() = name?.let { mapOf("activity" to it) }.orEmpty()

    override fun onActivityResumed(activity: Activity) {
        name = activity.javaClass.simpleName
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
