package mihon.desktop.di

import coil3.ImageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.player.WindowAnimePlayer
import mihon.desktop.player.mediacontrols.MprisAnimePlayerMediaControls
import mihon.desktop.player.mediacontrols.PlayerArtworkFiles
import mihon.desktop.player.pictureinpicture.ComposePictureInPictureWindow
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.window.WindowPreferences
import mihon.entry.interactions.player.AnimePlayerDestination
import mihon.entry.interactions.player.AnimePlayerMediaControls
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
