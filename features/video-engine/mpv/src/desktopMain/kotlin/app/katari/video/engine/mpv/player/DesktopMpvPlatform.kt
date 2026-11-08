package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.client.MpvSession
import app.katari.video.engine.mpv.client.MpvSessionHost
import app.katari.video.engine.mpv.proxy.DesktopLocalMediaFiles
import app.katari.video.engine.mpv.proxy.LocalMediaFiles
import app.katari.video.engine.mpv.session.DesktopMpvSession

/**
 * mpv on the desktop: the libmpv the app carries through JNA, an instance per player, drawing into Compose. The watched
 * player pauses when the sound's output goes away ([DesktopWatchedPlaybackHolds]).
 */
internal class DesktopMpvPlatform : MpvPlatform {
    override val localMedia: LocalMediaFiles = DesktopLocalMediaFiles()

    override fun openSession(host: MpvSessionHost): MpvSession = DesktopMpvSession(host)

    override fun holdPlayback(listener: WatchedPlaybackHolds.Listener): WatchedPlaybackHolds =
        DesktopWatchedPlaybackHolds(listener)
}
