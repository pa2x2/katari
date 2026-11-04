package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.player.WindowAnimePlayer
import mihon.desktop.player.pictureinpicture.ComposePictureInPictureWindow
import mihon.desktop.window.WindowPreferences
import mihon.entry.interactions.player.AnimePlayerDestination
import mihon.entry.interactions.player.AnimePlayerPictureInPictureWindow

@BindingContainer
object PlayerBindings {
    @Provides
    fun animePlayerDestination(player: WindowAnimePlayer): AnimePlayerDestination = player

    @Provides
    @SingleIn(AppScope::class)
    fun pictureInPictureWindow(
        preferences: WindowPreferences,
        displayBackend: DisplayBackend,
    ): AnimePlayerPictureInPictureWindow = ComposePictureInPictureWindow(preferences, displayBackend)
}
