package mihon.video.engine.mpv.player

import mihon.video.engine.mpv.client.MpvSession
import mihon.video.engine.mpv.client.MpvSessionHost
import mihon.video.engine.mpv.proxy.DesktopLocalMediaFiles
import mihon.video.engine.mpv.proxy.LocalMediaFiles
import mihon.video.engine.mpv.session.DesktopMpvSession

/**
 * mpv on the desktop: the libmpv the app carries through JNA, an instance per player, drawing into Compose. The watched
 * player holds nothing outside itself yet.
 */
internal class DesktopMpvPlatform : MpvPlatform {
    override val localMedia: LocalMediaFiles = DesktopLocalMediaFiles()

    override fun openSession(host: MpvSessionHost): MpvSession = DesktopMpvSession(host)

    override fun holdPlayback(listener: WatchedPlaybackHolds.Listener): WatchedPlaybackHolds = NoPlaybackHolds
}
