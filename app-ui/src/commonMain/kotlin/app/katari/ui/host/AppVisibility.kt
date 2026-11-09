package app.katari.ui.host

/**
 * The app showing and coming to the person's front, and leaving them, as each platform's host sees it. On Android both
 * follow the process lifecycle, so they move together. On a desktop the main window is shown while it is open and not
 * minimized, and is in front only while it has focus: it can sit open on another monitor or behind other windows, and
 * X11 doesn't tell whether those cover it.
 *
 * Sync keeps its live channel open while the app is shown, so an open window gets other devices' changes within
 * seconds (D021). It pulls when the app comes to the front and sends what is left when it goes (D021), and tells what
 * needs the person by notification only while the app is away from the front (D028).
 */
object AppVisibility {
    @HostEntryPoint
    fun becameShown(graph: AppProcessGraph) {
        graph.syncTriggers.appShown()
    }

    @HostEntryPoint
    fun becameHidden(graph: AppProcessGraph) {
        graph.syncTriggers.appHidden()
    }

    @HostEntryPoint
    fun cameToFront(graph: AppProcessGraph) {
        graph.syncNotices.cameToFront()
        graph.syncTriggers.appCameToFront()
    }

    @HostEntryPoint
    fun wentToBack(graph: AppProcessGraph) {
        graph.syncNotices.wentToBack()
        graph.syncTriggers.appWentToBack()
    }
}
