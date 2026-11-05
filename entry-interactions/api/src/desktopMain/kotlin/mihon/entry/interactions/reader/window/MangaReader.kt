package mihon.entry.interactions.reader.window

import androidx.compose.runtime.Composable

/** The manga reader on the desktop, which the app's [MangaReaderDestination] shows. */
interface MangaReader {
    /** The reader for [request], filling the screen it is composed in and leaving through [navigation]. */
    @Composable
    fun Content(request: MangaReaderRequest, navigation: MangaReaderNavigation)
}
