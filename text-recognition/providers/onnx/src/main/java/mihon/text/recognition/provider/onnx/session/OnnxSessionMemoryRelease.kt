package mihon.text.recognition.provider.onnx.session

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import kotlin.concurrent.thread

/**
 * Frees the loaded models once the app is in the background: their memory would otherwise make the system kill the
 * app sooner. Recognizing pages again reloads them. Closing sessions takes hundreds of milliseconds, so it runs off the
 * main thread.
 */
internal class OnnxSessionMemoryRelease(
    private val sessions: OnnxSessions,
) : ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            thread(name = "onnx-session-release") { sessions.releaseAll() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Android delivers this as onTrimMemory(TRIM_MEMORY_COMPLETE), which releases the models")
    override fun onLowMemory() = Unit
}
