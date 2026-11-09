package app.katari.entry.interactions.anime.runtime

import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.entry.interactions.anime.media.immersive.AnimeClipPlatform
import app.katari.entry.interactions.anime.media.immersive.AnimeImmersiveClips
import app.katari.entry.interactions.anime.media.immersive.AnimeImmersiveProcessor
import app.katari.entry.interactions.anime.media.immersive.PointerAnimeClipPlatform
import app.katari.entry.interactions.anime.navigation.AnimeOpenProcessor
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.player.AnimePlayer
import app.katari.entry.interactions.player.AnimePlayerDestination
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import app.katari.entry.interactions.runtime.EntryTypeKey
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.video.engine.mpv.MpvVideoEngine
import app.katari.video.engine.spi.VideoEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.DesktopVideoPlayer

/**
 * The desktop's player: episodes open in the screen the app's [AnimePlayerDestination] shows, and play in mpv, the
 * desktop's only engine (D001), as the immersive feed's clips do, taking the mouse and keys. Previews and a playback
 * cache stay Android's.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeDesktopPlayerBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun open(destination: AnimePlayerDestination): EntryOpenProcessor = AnimeOpenProcessor(destination)

    @Provides
    @SingleIn(AppScope::class)
    internal fun videoEngine(networkHelper: NetworkHelper): VideoEngine = MpvVideoEngine(networkHelper)

    @Provides
    internal fun player(player: DesktopVideoPlayer): AnimePlayer = player

    @Provides
    internal fun clipPlatform(
        inhibitor: ScreenIdleInhibitor,
        binder: ViewerSettingBinder,
        preferences: AnimePlayerPreferences,
        devicePreferences: AnimePlayerDevicePreferences,
    ): AnimeClipPlatform = PointerAnimeClipPlatform(inhibitor, binder, preferences, devicePreferences)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun immersive(clips: AnimeImmersiveClips): EntryImmersiveProcessor = AnimeImmersiveProcessor(clips)
}
