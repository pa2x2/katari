package mihon.video.engine.mpv.player

import android.app.Application
import android.os.Looper
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.client.MpvSession
import mihon.video.engine.mpv.client.MpvSessionHost
import mihon.video.engine.mpv.proxy.AndroidLocalMediaFiles
import mihon.video.engine.mpv.proxy.LocalMediaFiles
import mihon.video.engine.mpv.session.MpvInstance

/** mpv on Android: the add-on's libraries through `MPVLib`, the process's one instance, and Media3's playback holds. */
internal class AndroidMpvPlatform(
    private val context: Application,
    private val addon: MpvAddon,
) : MpvPlatform {
    override val localMedia: LocalMediaFiles = AndroidLocalMediaFiles(context)

    override fun openSession(host: MpvSessionHost): MpvSession = MpvInstance.open(context, addon, host)

    override fun holdPlayback(listener: WatchedPlaybackHolds.Listener): WatchedPlaybackHolds =
        AndroidWatchedPlaybackHolds(context, Looper.getMainLooper(), MpvInstance.looper, listener)
}
