package app.katari.entry.interactions.reader.window

import androidx.compose.runtime.Composable

/** The book reader on the desktop, which the app's [BookReaderDestination] shows. */
interface BookReader {
    /** The reader for [request], filling the screen it is composed in and leaving through [navigation]. */
    @Composable
    fun Content(request: BookReaderWindowRequest, navigation: BookReaderNavigation)
}
