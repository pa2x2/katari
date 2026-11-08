package app.katari.entry.interactions.player

import androidx.compose.runtime.Composable

/** The anime player on the desktop, which the app's [AnimePlayerDestination] shows. */
interface AnimePlayer {
    /**
     * The player for [request], leaving through [navigation], shown where [placement] says. It stays composed while a
     * screen it opened, such as its settings, covers it, and pauses then, as Android's player pauses under another
     * activity; in picture-in-picture it plays on in its own window.
     */
    @Composable
    fun Content(request: AnimePlayerRequest, navigation: AnimePlayerNavigation, placement: AnimePlayerPlacement)
}

/** Where the desktop player shows. */
enum class AnimePlayerPlacement {
    /** Filling the main window. */
    Window,

    /** Under a screen it opened, such as its settings: neither drawn nor given input. */
    Covered,

    /** In its picture-in-picture window ([AnimePlayerPictureInPictureWindow]), the main window showing other screens. */
    PictureInPicture,
}
