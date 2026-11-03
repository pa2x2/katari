package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import logcat.LogPriority
import mihon.video.engine.spi.audio.VolumeBoost
import tachiyomi.core.common.util.system.logcat
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * The watched ExoPlayer with a volume boost: Android's [LoudnessEnhancer] on the player's audio session, at the gain
 * mpv's volume scale gives the same level. The enhancer moves along when the player opens a new audio session, and it
 * only exists while the volume is boosted.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class BoostedExoPlayer(private val player: ExoPlayer) :
    ForwardingPlayer(player),
    VolumeBoost {

    private var boostPercent = VolumeBoost.NO_VOLUME_BOOST_PERCENT
    private var enhancer: LoudnessEnhancer? = null
    private val sessionListener = object : AnalyticsListener {
        override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
            releaseEnhancer()
            applyBoost()
        }
    }

    init {
        player.addAnalyticsListener(sessionListener)
    }

    override val canBoostVolume: Boolean
        get() = loudnessEnhancerAvailable

    override fun setVolumeBoost(percent: Int) {
        boostPercent = percent.coerceIn(VolumeBoost.NO_VOLUME_BOOST_PERCENT, VolumeBoost.MAX_VOLUME_BOOST_PERCENT)
        applyBoost()
    }

    override fun release() {
        player.removeAnalyticsListener(sessionListener)
        releaseEnhancer()
        super.release()
    }

    private fun applyBoost() {
        if (boostPercent == VolumeBoost.NO_VOLUME_BOOST_PERCENT || !canBoostVolume) {
            releaseEnhancer()
            return
        }
        val sessionId = player.audioSessionId.takeIf { it != C.AUDIO_SESSION_ID_UNSET } ?: return
        // The effect fails to start when the audio system is out of effect slots; playback goes on without the boost.
        try {
            val current = enhancer ?: LoudnessEnhancer(sessionId).also { enhancer = it }
            current.setTargetGain(gainMillibels(boostPercent))
            current.enabled = true
        } catch (e: RuntimeException) {
            logcat(LogPriority.WARN, e) { "Volume boost unavailable" }
            releaseEnhancer()
        }
    }

    private fun releaseEnhancer() {
        enhancer?.release()
        enhancer = null
    }
}

// mpv's mixer applies the cube of its volume level, which is 60·log10(level) in decibels.
private fun gainMillibels(percent: Int): Int =
    (MILLIBELS_PER_CUBED_DECADE * log10(percent / VolumeBoost.NO_VOLUME_BOOST_PERCENT.toDouble())).roundToInt()

private const val MILLIBELS_PER_CUBED_DECADE = 6_000

private val loudnessEnhancerAvailable: Boolean by lazy {
    AudioEffect.queryEffects().orEmpty().any { it.type == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER }
}
