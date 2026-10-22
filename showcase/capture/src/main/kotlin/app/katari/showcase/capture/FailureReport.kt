package app.katari.showcase.capture

import android.os.SystemClock
import app.katari.showcase.capture.ShowcaseDevice.FAILURE_DIRECTORY
import app.katari.showcase.capture.ShowcaseDevice.appId
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.shell
import app.katari.showcase.capture.ShowcaseDevice.writeDeviceFile
import java.io.ByteArrayOutputStream

/**
 * Saves what a failed scene leaves beside its screenshot, as `[name]-*.txt` in [FAILURE_DIRECTORY].
 *
 * Scenes find everything through the accessibility tree, and failed CI runs showed the app's window drawn on screen
 * while every query for its tree came back empty after a 5 s timeout. The report tells an app whose main thread is
 * stuck from a system that lost track of the window: the app's threads, sampled a few times so a thread stuck in one
 * place stands out from a busy one, system_server's threads, and the accessibility and window state.
 *
 * The pipeline runs adb as root, so shell commands may read other processes' threads and the app's private files.
 */
internal fun saveFailureReport(name: String) {
    val appPid = pidOf(appId)
    repeat(APP_THREAD_SAMPLES) { sample ->
        if (sample > 0) SystemClock.sleep(APP_THREAD_SAMPLE_INTERVAL_MS)
        save("$name-app-threads-$sample", appPid?.let { shell("debuggerd -j $it") } ?: "$appId is not running")
    }
    save("$name-system-threads", pidOf("system_server")?.let { shell("debuggerd -j $it") } ?: "no system_server")
    save("$name-hierarchy", hierarchy())
    save("$name-accessibility", shell("dumpsys accessibility"))
    save("$name-windows", shell("dumpsys window windows"))
}

/** The tree as the scene's own queries see it, which can differ from a fresh `uiautomator dump` of the same screen. */
private fun hierarchy(): String = ByteArrayOutputStream().use { out ->
    device.dumpWindowHierarchy(out)
    out.toString()
}

private fun pidOf(process: String): String? = shell("pidof $process").trim().split(' ').firstOrNull { it.isNotEmpty() }

private fun save(name: String, text: String) {
    writeDeviceFile("$FAILURE_DIRECTORY/$name.txt", text)
}

private const val APP_THREAD_SAMPLES = 3
private const val APP_THREAD_SAMPLE_INTERVAL_MS = 2_000L
