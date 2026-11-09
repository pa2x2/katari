package app.katari.desktop.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.entry.interactions.reader.window.MangaReaderRequest
import app.katari.ui.telemetry.EntryTypeScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen

/**
 * The reader's place in the window's navigation: the dock steps aside, back leaves it, and screens it opens go over
 * it. It holds the app's [WindowMangaReader] rather than reading it from the graph, as the desktop window never
 * restores its navigation from saved state.
 */
internal class MangaReaderScreen(
    private val request: MangaReaderRequest,
    private val windowReader: WindowMangaReader,
) : Screen(), FullWindowScreen, EntryTypeScreen {
    override val entryType: EntryType = EntryType.MANGA

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val navigation = remember(navigator) {
            WindowReaderNavigation(navigator, this, windowReader.webViewDestination)
        }
        windowReader.reader.Content(request, navigation)
    }
}
