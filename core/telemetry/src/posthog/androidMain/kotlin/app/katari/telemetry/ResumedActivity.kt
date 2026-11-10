package app.katari.telemetry

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlin.concurrent.Volatile

/**
 * The activity in front, which every event carries beside the screen the app's navigation shows: the readers, the
 * player and the settings run in activities of their own, outside that navigation. [onResumed] hears each one's name
 * as it comes to the front.
 */
internal class ResumedActivity(private val onResumed: (name: String) -> Unit) : Application.ActivityLifecycleCallbacks {
    @Volatile
    private var name: String? = null

    val properties: Map<String, Any>
        get() = name?.let { mapOf("activity" to it) }.orEmpty()

    override fun onActivityResumed(activity: Activity) {
        val resumed = activity.javaClass.simpleName
        name = resumed
        onResumed(resumed)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
