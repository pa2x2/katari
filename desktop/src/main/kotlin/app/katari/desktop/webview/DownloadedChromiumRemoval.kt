package app.katari.desktop.webview

import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.storage.DesktopDirectories
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.io.path.exists

/**
 * Removes the Chromium that desktop 1.0.0 to 1.0.2 downloaded into the data folder, with what was left of its
 * download: about 350 MB nothing reads, as the app runs the Chromium its Java runtime carries. A removal cut short by
 * the app ending goes on at the next start.
 */
@Inject
class DownloadedChromiumRemoval(
    private val directories: DesktopDirectories,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch(Dispatchers.IO) {
            val folder = directories.data.resolve(FOLDER_NAME)
            if (folder.exists() && !folder.toFile().deleteRecursively()) {
                logcat(LogPriority.WARN) { "The downloaded Chromium in $folder couldn't be removed whole" }
            }
        }
    }

    private companion object {
        const val FOLDER_NAME = "chromium"
    }
}
