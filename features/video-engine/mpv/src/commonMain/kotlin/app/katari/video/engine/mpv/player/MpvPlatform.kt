package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.client.MpvSession
import app.katari.video.engine.mpv.client.MpvSessionHost
import app.katari.video.engine.mpv.proxy.LocalMediaFiles

/**
 * What [MpvPlayer] needs from the platform it plays on: an mpv instance of its own, bound to the platform's libmpv and
 * its picture output; what the watched player holds outside itself while it plays; and the downloaded files streams
 * come from.
 */
internal interface MpvPlatform {
    val localMedia: LocalMediaFiles

    fun openSession(host: MpvSessionHost): MpvSession

    fun holdPlayback(listener: WatchedPlaybackHolds.Listener): WatchedPlaybackHolds
}
