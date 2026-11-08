package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.player.WindowAnimePlayer
import app.katari.desktop.player.mediacontrols.MprisAnimePlayerMediaControls
import app.katari.desktop.player.mediacontrols.PlayerArtworkFiles
import app.katari.desktop.player.pictureinpicture.ComposePictureInPictureWindow
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.window.WindowPreferences
import app.katari.entry.interactions.player.AnimePlayerDestination
import app.katari.entry.interactions.player.AnimePlayerMediaControls
import app.katari.entry.interactions.player.AnimePlayerPictureInPictureWindow
import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

@BindingContainer
object PlayerBindings {
    @Provides
    fun animePlayerDestination(player: WindowAnimePlayer): AnimePlayerDestination = player

    @Provides
    @SingleIn(AppScope::class)
    fun pictureInPictureWindow(preferences: WindowPreferences): AnimePlayerPictureInPictureWindow =
        ComposePictureInPictureWindow(preferences)

    @Provides
    @SingleIn(AppScope::class)
    fun mediaControls(
        operatingSystem: DesktopOperatingSystem,
        imageLoader: ImageLoader,
        directories: DesktopDirectories,
        @AppCoroutineScope scope: CoroutineScope,
    ): AnimePlayerMediaControls = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> MprisAnimePlayerMediaControls(
            PlayerArtworkFiles(imageLoader, directories, scope),
        )
    }
}
