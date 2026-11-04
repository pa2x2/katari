package mihon.video.engine.mpv.player

import mihon.video.engine.mpv.client.MpvSession
import mihon.video.engine.mpv.client.MpvSessionHost
import mihon.video.engine.mpv.proxy.LocalMediaFiles

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
