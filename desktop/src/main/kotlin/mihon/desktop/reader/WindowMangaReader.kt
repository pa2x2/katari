package mihon.desktop.reader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.core.platform.PlatformContext
import mihon.desktop.window.WindowPlatformContext
import mihon.entry.interactions.reader.window.MangaReader
import mihon.entry.interactions.reader.window.MangaReaderDestination
import mihon.entry.interactions.reader.window.MangaReaderRequest

/**
 * The manga reader in the app's window: a screen of the window's navigation that the reader fills
 * ([MangaReaderScreen]). A chapter opened while the reader is on top, as from a notification, takes its place.
 */
@SingleIn(AppScope::class)
@Inject
class WindowMangaReader(
    internal val reader: MangaReader,
    internal val webViewDestination: WebViewDestination,
) : MangaReaderDestination {
    override fun open(context: PlatformContext, request: MangaReaderRequest) {
        val navigator = (context as? WindowPlatformContext)?.navigator
            ?: error("The reader opens only from a window that shows the app's screens")
        val screen = MangaReaderScreen(request, this)
        if (navigator.lastItem is MangaReaderScreen) navigator.replace(screen) else navigator.push(screen)
    }
}
