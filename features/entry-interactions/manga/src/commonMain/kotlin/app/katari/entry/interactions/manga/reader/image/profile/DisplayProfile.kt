package app.katari.entry.interactions.manga.reader.image.profile

import app.katari.core.image.progressive.ProgressiveImageDecodeOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.core.common.util.system.logcat
import java.io.IOException

/**
 * The screen's ICC profile that manga pages are converted to while they decode, or null to convert nothing. Pages
 * decode in the readers' viewers, the immersive view and the image loader, none of which has the graph at hand, so the
 * profile is kept here for the process, and [follow] keeps it to the colour profile setting from the app's start.
 */
internal object DisplayProfile {
    @Volatile
    var bytes: ByteArray? = null
        private set

    /** Reads the file [setting] names whenever it changes; an empty setting or an unreadable file clears the profile. */
    fun follow(setting: Preference<String>, files: StorageFileResolver, scope: CoroutineScope) {
        setting.changes()
            .map { uri -> read(uri, files) }
            .flowOn(Dispatchers.IO)
            .onEach { bytes = it }
            .launchIn(scope)
    }

    private fun read(uri: String, files: StorageFileResolver): ByteArray? {
        if (uri.isEmpty()) return null
        val file = files.fromUri(uri)?.takeIf { it.exists() }
        if (file == null) {
            logcat(LogPriority.WARN) { "Display profile $uri not found" }
            return null
        }
        // The decoders refuse a larger profile, and a file that large is no profile
        if (file.length() > ProgressiveImageDecodeOptions.MAXIMUM_DISPLAY_PROFILE_BYTES) {
            logcat(LogPriority.WARN) { "Display profile $uri is too large to be one" }
            return null
        }
        return try {
            file.openInputStream().use { it.readBytes() }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't read display profile $uri" }
            null
        }
    }
}
