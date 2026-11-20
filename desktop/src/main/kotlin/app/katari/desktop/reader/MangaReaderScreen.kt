package app.katari.desktop.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.reader.window.MangaReaderRequest
import app.katari.presentation.kit.navigation.FullWindowContent
import app.katari.ui.telemetry.ReportEntryType
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * The reader's place in the window's navigation: the dock steps aside, back leaves it, and screens it opens go over
 * it. It holds the app's [WindowMangaReader] rather than reading it from the graph, as the desktop window never
 * restores its navigation from saved state.
 */
internal class MangaReaderScreen(
    private val request: MangaReaderRequest,
    private val windowReader: WindowMangaReader,
) : Screen() {
    @Composable
    override fun Content() {
        ReportEntryType(this, EntryType.MANGA)
        val navigator = LocalNavigator.currentOrThrow
        val navigation = remember(navigator) {
            WindowReaderNavigation(navigator, this, windowReader.webViewDestination)
        }
        FullWindowContent(Modifier.fillMaxSize()) { windowReader.reader.Content(request, navigation) }
    }
}
