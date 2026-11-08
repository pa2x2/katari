package app.katari.extension.runtime.jvm.application

import android.app.Activity
import android.app.ActivityThread

/**
 * The `Activity` extensions find in the foreground on desktop, standing for the app's window: launched as the window
 * opens and destroyed as it closes, on the main looper's thread, so the application's lifecycle callbacks and
 * `ActivityThread`, which extensions read to find it, see it as Android's resumed activity. Their dialogs over it open
 * in the window. A run without a window, such as a developer tool's, has no activity, as an app in the background has
 * none in the foreground.
 */
class WindowActivity(private val application: ExtensionApplication) {
    // On the main looper's thread
    private var activity: Activity? = null

    fun onWindowOpened() {
        MainLooperThread.execute {
            if (activity == null) activity = ActivityThread.currentActivityThread().launchActivity(application)
        }
    }

    fun onWindowClosed() {
        MainLooperThread.execute {
            activity?.let(ActivityThread.currentActivityThread()::destroyActivity)
            activity = null
        }
    }
}
