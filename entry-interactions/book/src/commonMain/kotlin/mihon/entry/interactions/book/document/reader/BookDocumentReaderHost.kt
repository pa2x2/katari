package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.geometry.Rect
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution

/**
 * What the book reader gets from the platform that hosts it: what the platform does its own way, what it can't do, and
 * the actions that reach outside the reader.
 */
internal class BookDocumentReaderHost(
    /** The window the reader is in, for its strings and the links and pictures it hands out. */
    val context: PlatformContext,
    /** What the chapter's web menu offers, which leaves out what the platform can't do, such as sharing on desktop. */
    val webViewActions: List<EntryChildWebViewAction>,
    /** Whether the selection menu offers Listen, which needs a speech engine the desktop doesn't have yet. */
    val speaksSelections: Boolean,
    /** Whether the window has status and navigation bars the reader can show or hide, which a desktop window doesn't. */
    val systemBars: Boolean,
    /**
     * Whether selected text gets the platform's floating toolbar, as on Android, whose own actions the Show text
     * selection menu setting keeps or leaves out. Without one, as on the desktop, a right click on the selection offers
     * only the reader's actions, and the setting is hidden.
     */
    val selectionToolbar: Boolean,
    /** Shows a short message about what the reader just did, outside its own snackbars. */
    val showMessage: (String) -> Unit,
    val onClose: () -> Unit,
    /** Opens the chapter's web page as [EntryChildWebViewAction] says, titled with the book's title. */
    val onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available, title: String?) -> Unit,
    /** The reader's own settings screen, from the settings sheet's menu. */
    val onOpenDefaultSettings: () -> Unit,
    /**
     * Opens the reader again where it is, with a new controller over the same retained sessions, as new text rules
     * apply to chapters only as they open.
     */
    val onReopen: () -> Unit,
    /** Where the translation popup is on screen, or null once it closes. */
    val onTranslationPopupBoundsChanged: (Rect?) -> Unit,
)
