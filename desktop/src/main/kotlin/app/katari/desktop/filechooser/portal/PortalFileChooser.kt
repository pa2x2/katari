package app.katari.desktop.filechooser.portal

import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.Variant
import app.katari.desktop.display.backend.DisplayBackend
import app.katari.desktop.portal.DesktopPortal
import app.katari.desktop.portal.PortalInterface
import app.katari.desktop.portal.PortalRequest
import app.katari.desktop.portal.PortalResponse
import app.katari.desktop.portal.portalParentWindow
import app.katari.presentation.kit.files.DocumentPickerRequest
import app.katari.presentation.kit.files.SystemFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.storage.DocumentUri
import java.awt.Window
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Path

/**
 * Linux: files and folders chosen in the desktop's own dialog, which the XDG desktop portal's file chooser shows
 * through the desktop's portal backend (GNOME's, KDE's, or GTK's on desktops without one of their own), over the app's
 * window (see [portalParentWindow]).
 *
 * At the first choice the bus is asked, once for the run, whether the portal is there, whether it offers the file
 * chooser, and in which version. Bare window managers often run no portal, or only backends without a file chooser
 * (`xdg-desktop-portal-wlr` has none); then [fallback] chooses, as it does what the portal's version can't, such as a
 * folder before version 3. When the portal answers a later call that the file chooser isn't there, as after its
 * backend has quit, [fallback] chooses from then on. Any other failure is no choice, and is logged: it may have
 * happened in a dialog the user already saw.
 */
internal class PortalFileChooser(
    private val fallback: SystemFileChooser,
) : SystemFileChooser {
    private val probe = Mutex()

    // The portal's file chooser version; null until the first choice asks, 0 where there is none to use
    private var version: Int? = null

    override suspend fun choose(parent: Window?, request: DocumentPickerRequest, suggestedName: String?): DocumentUri? {
        val call = fileChooserCall(request, suggestedName)
        if (portalVersion() < call.minimumVersion) return fallback.choose(parent, request, suggestedName)
        val response = try {
            PortalRequest.send(
                FILE_CHOOSER,
                call.method,
                signature = "ss",
                // No title, as the Java dialogs had none: a request carries no words for one
                arguments = listOf(portalParentWindow(parent), ""),
                options = call.options,
            )
        } catch (e: DbusException) {
            if (DesktopPortal.isMissing(e)) {
                probe.withLock { version = 0 }
                logcat(LogPriority.WARN, e) { "The file chooser portal is gone; files are chosen in Java's dialogs" }
                return fallback.choose(parent, request, suggestedName)
            }
            logcat(LogPriority.ERROR, e) { "The file chooser portal refused ${call.method}" }
            return null
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Lost the file chooser portal during ${call.method}" }
            return null
        }
        return when (response) {
            is PortalResponse.Done -> chosenDocument(response.results)
            PortalResponse.Cancelled -> null
            PortalResponse.Ended -> {
                logcat(LogPriority.ERROR) { "The file chooser portal ended ${call.method} without a choice" }
                null
            }
        }
    }

    /**
     * What the user chose, from the `uris` of the response's [results], as a `file://` URI written the way the app
     * writes its own; null when they hold none. The portal answers with `file://` URIs: real paths for an app outside a
     * sandbox, paths in the document portal's `/run/user/…/doc` for a sandboxed one.
     */
    private fun chosenDocument(results: Map<String, Variant>): DocumentUri? {
        val uri = (results["uris"]?.value as? List<*>)?.firstOrNull() as? String ?: return null
        return try {
            DocumentUri(Path.of(URI(uri)).toUri().toString())
        } catch (e: URISyntaxException) {
            logcat(LogPriority.ERROR, e) { "The file chooser portal answered with a URI that isn't one: $uri" }
            null
        } catch (e: IllegalArgumentException) {
            logcat(LogPriority.ERROR, e) { "The file chooser portal answered with a URI that isn't a file's: $uri" }
            null
        } catch (e: FileSystemNotFoundException) {
            logcat(LogPriority.ERROR, e) { "The file chooser portal answered with a URI that isn't a file's: $uri" }
            null
        }
    }

    private suspend fun portalVersion(): Int = probe.withLock {
        version ?: withContext(Dispatchers.IO) { DesktopPortal.probe(FILE_CHOOSER) }.let { answer ->
            val found = when (answer) {
                is PortalInterface.Offered -> answer.version
                is PortalInterface.Missing -> 0
            }
            logcat(LogPriority.INFO) {
                when {
                    answer is PortalInterface.Missing -> "Files are chosen in Java's dialogs: ${answer.reason}"
                    found < FILE_CHOOSER_FOLDER_VERSION ->
                        "Files are chosen through the file chooser portal, version $found, and folders, which it " +
                            "can't choose before version $FILE_CHOOSER_FOLDER_VERSION, in Java's dialogs"
                    else -> "Files and folders are chosen through the file chooser portal, version $found"
                }
            }
            version = found
            found
        }
    }
}
