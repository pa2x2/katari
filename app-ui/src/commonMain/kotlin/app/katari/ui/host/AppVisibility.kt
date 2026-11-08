package app.katari.ui.host

/**
 * The app coming to the person's front and leaving it, as each platform's host sees it: Android's process lifecycle,
 * a desktop window opening, gaining focus or closing. Sync pulls when the app comes to the front and sends what is
 * left when it goes (D021), and tells what needs the person by notification only while the app is away (D028).
 */
object AppVisibility {
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
