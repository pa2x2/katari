package app.katari.ui.host

/**
 * The app opening and coming to the person's front, and leaving them, as each platform's host sees it. On Android both
 * follow the process lifecycle, so they move together. On a desktop the app is open while its window is, minimized or
 * not, and is in front only while the window has focus.
 *
 * Sync keeps its live channel open while the app is open, so an open window gets other devices' changes within seconds
 * (D021). It pulls when the app comes to the front and sends what is left when it goes (D021), and tells what
 * needs the person by notification only while the app is away from the front (D028).
 */
object AppVisibility {
    @HostEntryPoint
    fun opened(graph: AppProcessGraph) {
        graph.syncTriggers.appOpened()
    }

    @HostEntryPoint
    fun closed(graph: AppProcessGraph) {
        graph.syncTriggers.appClosed()
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
