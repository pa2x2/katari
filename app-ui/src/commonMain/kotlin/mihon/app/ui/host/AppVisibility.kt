package mihon.app.ui.host

/**
 * The app coming to the person's front and leaving it, as each platform's host sees it: Android's process lifecycle,
 * a desktop window opening, gaining focus or closing. Sync pulls when the app comes to the front and sends what is
 * left when it goes (D021).
 */
object AppVisibility {
    @HostEntryPoint
    fun cameToFront(graph: AppProcessGraph) {
        graph.syncTriggers.appCameToFront()
    }

    @HostEntryPoint
    fun wentToBack(graph: AppProcessGraph) {
        graph.syncTriggers.appWentToBack()
    }
}
