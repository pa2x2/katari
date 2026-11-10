package app.katari.desktop.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.entry.interactions.reader.window.BookReaderWindowRequest
import app.katari.ui.telemetry.ReportEntryType
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen

/**
 * The book reader's place in the window's navigation: the dock steps aside, back leaves it, and screens it opens go
 * over it. It holds the app's [WindowBookReader] rather than reading it from the graph, as the desktop window never
 * restores its navigation from saved state.
 */
internal class BookReaderScreen(
    private val request: BookReaderWindowRequest,
    private val windowReader: WindowBookReader,
) : Screen(), FullWindowScreen {
    @Composable
    override fun Content() {
        ReportEntryType(this, EntryType.BOOK)
        val navigator = LocalNavigator.currentOrThrow
        val navigation = remember(navigator) {
            WindowBookReaderNavigation(navigator, this, windowReader.webViewDestination)
        }
        windowReader.reader.Content(request, navigation)
    }
}
