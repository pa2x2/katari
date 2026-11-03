package eu.kanade.tachiyomi.ui.entry.open

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.app.ui.di.LocalAppUiGraph
import mihon.entry.interactions.navigation.EntryContinueResult
import mihon.entry.interactions.navigation.EntryOpenResult
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.presentation.kit.feedback.ScreenMessages
import mihon.presentation.kit.feedback.rememberScreenMessages

/**
 * Tells the user, on the screen they are on, that a chapter or episode they opened or continued with didn't open
 * because this platform has no reader or player for its type yet. Every screen that opens or continues an entry
 * reports the answer here, so they all say the same. Report from the main thread.
 */
@Stable
class EntryOpenMessages internal constructor(
    private val messages: ScreenMessages,
    private val presentation: EntryTypePresentationFeature,
) {
    fun report(type: EntryType, result: EntryOpenResult) {
        if (result == EntryOpenResult.ViewerUnavailable) showViewerUnavailable(type)
    }

    fun report(type: EntryType, result: EntryContinueResult) {
        if (result == EntryContinueResult.ViewerUnavailable) showViewerUnavailable(type)
    }

    private fun showViewerUnavailable(type: EntryType) {
        messages.show(type.entryTypePresentation(presentation).viewerUnavailableLabel)
    }
}

@Composable
fun rememberEntryOpenMessages(): EntryOpenMessages {
    val messages = rememberScreenMessages()
    val presentation = LocalAppUiGraph.current.entryTypePresentationFeature
    return remember(messages, presentation) { EntryOpenMessages(messages, presentation) }
}
