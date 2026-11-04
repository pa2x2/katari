package mihon.entry.interactions.player

import androidx.compose.runtime.Composable

/** The anime player on the desktop, which the app's [AnimePlayerDestination] shows as a screen of its window. */
interface AnimePlayer {
    /** The player for [request], leaving through [navigation]. */
    @Composable
    fun Content(request: AnimePlayerRequest, navigation: AnimePlayerNavigation)
}
