@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import android.app.Activity
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.BookReaderTextSelection
import mihon.entry.interactions.book.BookSelectionTranslationController
import org.readium.r2.navigator.epub.EpubNavigatorFragment

internal class ReadiumEpubSelectionCoordinator(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val controller: BookSelectionTranslationController,
    private val navigator: () -> EpubNavigatorFragment?,
    private val isFixedLayout: () -> Boolean?,
    private val readerRootPositionInWindow: () -> Offset,
) : AutoCloseable {
    private var resolutionJob: Job? = null
    private var listenerInstallationJob: Job? = null

    val changeBridge = ReadiumSelectionChangeBridge(::onSelectionChanged)

    fun installListener() {
        val fragment = navigator() ?: return
        if (isFixedLayout() != false) return
        listenerInstallationJob?.cancel()
        listenerInstallationJob = scope.launch {
            fragment.evaluateJavascript(INSTALL_READIUM_SELECTION_LISTENER_SCRIPT)
        }
    }

    fun clearSelection() {
        controller.clearSelection()
    }

    private fun onSelectionChanged() {
        activity.runOnUiThread {
            if (!controller.effectiveEnabled.value) return@runOnUiThread
            val fragment = navigator() ?: return@runOnUiThread
            resolutionJob?.cancel()
            resolutionJob = scope.launch {
                val selection = try {
                    fragment.currentSelection()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                val text = selection?.locator?.text?.highlight
                if (selection == null || text.isNullOrBlank()) {
                    controller.clearSelection()
                    return@launch
                }
                val navigatorView = fragment.publicationView
                val navigatorPosition = IntArray(2).also(navigatorView::getLocationInWindow)
                val anchor = selection.rect?.toReaderRootAnchor(
                    navigatorViewPositionInWindow = Offset(
                        navigatorPosition[0].toFloat(),
                        navigatorPosition[1].toFloat(),
                    ),
                    readerRootPositionInWindow = readerRootPositionInWindow(),
                    readiumContentTopInset = navigatorView.readiumSelectionContentTopInset(),
                )
                val resourceIdentity = selection.locator.href.toString()
                val selectionIdentity = "$resourceIdentity:${selection.locator.text.hashCode()}"
                controller.submitSelection(
                    BookReaderTextSelection(
                        ownerIdentity = resourceIdentity,
                        identity = selectionIdentity,
                        text = text,
                        anchor = anchor,
                    ),
                )
            }
        }
    }

    override fun close() {
        resolutionJob?.cancel()
        resolutionJob = null
        listenerInstallationJob?.cancel()
        listenerInstallationJob = null
    }
}
