package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.DesktopVideoPlayer
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.interactions.anime.media.immersive.AnimeClipPlatform
import mihon.entry.interactions.anime.media.immersive.AnimeImmersiveClips
import mihon.entry.interactions.anime.media.immersive.AnimeImmersiveProcessor
import mihon.entry.interactions.anime.media.immersive.PointerAnimeClipPlatform
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.player.AnimePlayer
import mihon.entry.interactions.player.AnimePlayerDestination
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.video.engine.mpv.MpvVideoEngine
import mihon.video.engine.spi.VideoEngine

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
    ): AnimeClipPlatform = PointerAnimeClipPlatform(inhibitor, binder, preferences)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun immersive(clips: AnimeImmersiveClips): EntryImmersiveProcessor = AnimeImmersiveProcessor(clips)
}
