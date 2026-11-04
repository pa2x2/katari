package mihon.entry.interactions.player

import androidx.compose.runtime.Composable

/** The anime player on the desktop, which the app's [AnimePlayerDestination] shows as a screen of its window. */
interface AnimePlayer {
    /**
     * The player for [request], leaving through [navigation]. It stays composed while a screen it opened, such as its
     * settings, covers it ([covered]), and pauses then, as Android's player pauses under another activity.
     */
    @Composable
    fun Content(request: AnimePlayerRequest, navigation: AnimePlayerNavigation, covered: Boolean)
}
