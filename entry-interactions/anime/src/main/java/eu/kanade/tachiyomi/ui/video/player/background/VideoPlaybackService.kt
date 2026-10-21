package eu.kanade.tachiyomi.ui.video.player.background

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import mihon.entry.interactions.media.playback.VideoPlaybackNotifications
import tachiyomi.i18n.*
import mihon.icons.materialsymbols.R as MaterialSymbolsR

/**
 * Keeps an episode playing after the player leaves the screen. VideoPlayerActivity still owns the player and its
 * session; this service only hosts that session, which gives it the media notification and lets it run in the
 * foreground while it plays. The activity starts it with [host] and stops it with [stopHosting] when it releases the
 * session, so the service never outlives the player.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class VideoPlaybackService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(VideoPlaybackNotifications.CHANNEL)
                .setChannelName(MR.strings.channel_video_playback.resourceId)
                .setNotificationId(VideoPlaybackNotifications.ID)
                .build()
                .apply { setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_play_arrow) },
        )
        hostedSession?.let(::addSession) ?: stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = hostedSession

    companion object {
        // Set only between host and stopHosting, which the activity calls on the main thread.
        private var hostedSession: MediaSession? = null

        internal fun host(context: Context, session: MediaSession) {
            // Once per session: episodes can change while the app is in the background, where it may not start one.
            if (hostedSession === session) return
            hostedSession = session
            context.startService(Intent(context, VideoPlaybackService::class.java))
        }

        internal fun stopHosting(context: Context) {
            if (hostedSession == null) return
            hostedSession = null
            context.stopService(Intent(context, VideoPlaybackService::class.java))
        }
    }
}
