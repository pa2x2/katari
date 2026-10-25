package mihon.video.engine.mpv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.audio.AudioFocusManager
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.source.entry.VideoStream
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEndFileReason
import `is`.xyz.mpv.MPVLib.MpvError
import `is`.xyz.mpv.MPVLib.MpvEvent
import `is`.xyz.mpv.MPVLib.MpvLogLevel
import logcat.LogPriority
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.proxy.StreamProxy
import mihon.video.engine.mpv.proxy.UpstreamFailure
import mihon.video.engine.mpv.proxy.hls.HlsVariantPick
import mihon.video.engine.mpv.proxy.hls.HlsVariantSet
import mihon.video.engine.mpv.quality.MpvBandwidthMeter
import mihon.video.engine.mpv.quality.adaptiveVideoTrack
import mihon.video.engine.mpv.quality.initialBitrateCap
import mihon.video.engine.mpv.session.MpvInstance
import mihon.video.engine.mpv.session.MpvSessionHost
import mihon.video.engine.mpv.tracks.MpvExternalAudio
import mihon.video.engine.mpv.tracks.MpvExternalSubtitle
import mihon.video.engine.mpv.tracks.MpvTrack
import mihon.video.engine.mpv.tracks.MpvTrackKind
import mihon.video.engine.mpv.tracks.MpvTrackRequest
import mihon.video.engine.mpv.tracks.mpvMedia3Tracks
import mihon.video.engine.mpv.tracks.mpvTrackChoices
import mihon.video.engine.mpv.tracks.toMpvTrackRequest
import mihon.video.engine.spi.VideoPlayerRole
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.loading.resolvedRequest
import mihon.video.engine.spi.picture.SelfPlacedPicture
import mihon.video.engine.spi.tracks.SelfDrawnSubtitles
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import kotlin.math.log2

/**
 * The watched player on mpv: one stream with its external subtitles and audio, through a [StreamProxy], on the
 * process's one mpv instance ([MpvInstance]). It publishes mpv's state as Media3's, so the player screen, the media
 * session and background playback use it as they use ExoPlayer.
 *
 * The state lives on the main thread. mpv is driven from the mpv thread and reports on its event thread, whose
 * property changes come back here, except the playback position, which changes every frame and is read as it is.
 *
 * Subtitles mpv can't hand over as text, or that carry their own styling (ASS), mpv draws itself. Every other
 * subtitle is hidden in mpv and published as [currentCues], so it looks as it does on ExoPlayer.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class MpvPlayer(
    context: Context,
    private val addon: MpvAddon,
    private val client: OkHttpClient,
    private val stream: VideoStream,
    private val headers: VideoRequestHeaders,
    private val role: VideoPlayerRole.Watched,
    private val bandwidthMeter: MpvBandwidthMeter,
) : SimpleBasePlayer(Looper.getMainLooper()),
    SelfDrawnSubtitles,
    SelfPlacedPicture {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val mediaItem = MediaItem.Builder()
        .setUri(stream.resolvedRequest.url)
        .setMediaMetadata(role.metadata)
        .build()

    // Main thread.
    private var released = false
    private var playWhenReady = false
    private var playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
    private var prepared = false
    private var everLoaded = false
    private var loaded = false
    private var restarted = false
    private var newlyRenderedFirstFrame = false
    private var firstFrameReported = false
    private var pausedForCache = false
    private var seeking = false
    private var endReached = false
    private var error: PlaybackException? = null
    private var durationMs = C.TIME_UNSET
    private var seekable = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var tracks = Tracks.EMPTY
    private var sentTrackChoices = emptyMap<MpvTrackKind, String>()

    /** The playing set of variants and what was asked of it, since the file loaded. */
    private var appliedVariantPick: Pair<Int, HlsVariantPick>? = null

    private var playbackParameters = PlaybackParameters.DEFAULT
    private var volume = 1f
    private var cues = CueGroup.EMPTY_TIME_ZERO
    private var mpvDrawsSubtitle = false
    private var sentPause: Boolean? = null
    private var externalSubtitles = emptyList<MpvExternalSubtitle>()
    private var externalAudio = emptyList<MpvExternalAudio>()
    private var upstreamFailures = emptyList<UpstreamFailure>()
    private var variantSets: () -> List<HlsVariantSet> = { emptyList() }

    /** Why the instance couldn't start, which ends the session for good. */
    private var startFailure: PlaybackException? = null

    /** Where the current load started. */
    private var loadStartMs = 0L

    /** The position asked for that mpv hasn't reached yet: a seek, or the start of the next load. */
    private var pendingPositionMs: Long? = null

    // Written on the main thread, read on the proxy's too: it asks what to play from a set of variants.
    @Volatile
    private var mpvTracks = emptyList<MpvTrack>()

    @Volatile
    private var trackSelectionParameters = TrackSelectionParameters.Builder().build()

    // Written on mpv's threads, read anywhere.
    @Volatile
    private var positionMs = 0L

    @Volatile
    private var bufferedMs = 0L

    /** The playlist entry of the current load; the end of an earlier one is stale. */
    @Volatile
    private var entryId = -1L

    private val errorLog = ArrayDeque<String>()

    /**
     * Auto's quality follows the network: the choice is weighed again while a file is loaded. Within a set of variants
     * the proxy chooses each segment's, and the tracks follow the variant that plays.
     */
    private val adaptQuality = object : Runnable {
        override fun run() {
            if (released || !loaded) return
            applyTrackChoices()
            val updated = media3Tracks()
            if (updated != tracks) {
                tracks = updated
                invalidateState()
            }
            main.postDelayed(this, ADAPT_INTERVAL_MS)
        }
    }

    /** Confined to the mpv thread. */
    private var proxy: StreamProxy? = null

    private val holds = WatchedPlaybackHolds(
        appContext,
        Looper.getMainLooper(),
        MpvInstance.looper,
        AUDIO_ATTRIBUTES,
        HoldsListener(),
    )

    // Last, because the mpv thread starts using the fields above as soon as the session opens.
    private val session = MpvInstance.open(appContext, Host())
    private val output = MpvVideoOutput(session)

    override val drawsSelectedSubtitle: Boolean get() = mpvDrawsSubtitle

    // mpv's zoom is a power of two, and its pan is in the scaled picture's size, as asked; both outlast a reload.
    override fun placePicture(scale: Double, offsetX: Double, offsetY: Double) {
        session.post {
            MPVLib.setPropertyDouble("video-zoom", log2(scale))
            MPVLib.setPropertyDouble("video-pan-x", offsetX)
            MPVLib.setPropertyDouble("video-pan-y", offsetY)
        }
    }

    override fun getState(): State {
        val playbackState = playbackState()
        val item = MediaItemData.Builder(MEDIA_ITEM_UID)
            .setMediaItem(mediaItem)
            .setMediaMetadata(role.metadata)
            .setTracks(tracks)
            .setDurationUs(if (durationMs == C.TIME_UNSET) C.TIME_UNSET else Util.msToUs(durationMs))
            .setIsSeekable(seekable)
            .setIsDynamic(false)
            .build()
        val firstFrame = newlyRenderedFirstFrame
        newlyRenderedFirstFrame = false
        return State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlayWhenReady(playWhenReady, playWhenReadyReason)
            .setPlaybackState(playbackState)
            .setPlaybackSuppressionReason(
                if (playWhenReady && holds.focusCommand == AudioFocusManager.PLAYER_COMMAND_WAIT_FOR_CALLBACK) {
                    Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                } else {
                    Player.PLAYBACK_SUPPRESSION_REASON_NONE
                },
            )
            .setPlayerError(error)
            .setIsLoading(playbackState == Player.STATE_BUFFERING)
            .setSeekBackIncrementMs(role.seekStepMs)
            .setSeekForwardIncrementMs(role.seekStepMs)
            .setPlaybackParameters(playbackParameters)
            .setTrackSelectionParameters(trackSelectionParameters)
            .setAudioAttributes(AUDIO_ATTRIBUTES)
            .setVolume(volume)
            .setVideoSize(videoSize())
            .setCurrentCues(cues)
            .setNewlyRenderedFirstFrame(firstFrame)
            .setPlaylist(listOf(item))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs { pendingPositionMs ?: positionMs }
            .setContentBufferedPositionMs { maxOf(bufferedMs, pendingPositionMs ?: positionMs) }
            .build()
    }

    private fun videoSize(): VideoSize =
        if (videoWidth > 0 && videoHeight > 0) VideoSize(videoWidth, videoHeight) else VideoSize.UNKNOWN

    private fun playbackState(): Int = when {
        error != null || !prepared -> Player.STATE_IDLE
        endReached -> Player.STATE_ENDED
        !loaded || !restarted || pausedForCache || seeking || pendingPositionMs != null -> Player.STATE_BUFFERING
        else -> Player.STATE_READY
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
        holds.setPlayWhenReady(playWhenReady, playbackState())
        applyPause()
        return DONE
    }

    override fun handlePrepare(): ListenableFuture<*> {
        // Nothing would ever load: the session is over.
        startFailure?.let {
            fail(it)
            return DONE
        }
        val start = pendingPositionMs ?: if (everLoaded) positionMs else 0L
        error = null
        prepared = true
        loaded = false
        restarted = false
        endReached = false
        loadStartMs = start
        pendingPositionMs = start
        sentTrackChoices = emptyMap()
        appliedVariantPick = null
        val pause = !playsNow()
        sentPause = pause
        session.post {
            val proxy = proxy ?: return@post
            MPVLib.setPropertyBoolean("pause", pause)
            MPVLib.setOptionString("start", seconds(start))
            val options = loadfileOptions(streamFileOptions(stream, initialBitrateCap(bandwidthMeter.bitrateEstimate)))
            // -1 is loadfile's playlist index, which "replace" ignores but mpv needs before the options.
            MPVLib.command(
                if (options == null) {
                    arrayOf("loadfile", proxy.streamUrl, "replace")
                } else {
                    arrayOf("loadfile", proxy.streamUrl, "replace", "-1", options)
                },
            )
            MPVLib.getPropertyInt("playlist/0/id")?.let { entryId = it.toLong() }
        }
        refresh()
        return DONE
    }

    override fun handleStop(): ListenableFuture<*> {
        prepared = false
        loaded = false
        endReached = false
        session.post { MPVLib.command(arrayOf("stop")) }
        refresh()
        return DONE
    }

    override fun handleRelease(): ListenableFuture<*> {
        released = true
        main.removeCallbacks(adaptQuality)
        holds.release()
        output.release()
        session.close {
            proxy?.close()
            proxy = null
        }
        return DONE
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val target = if (positionMs == C.TIME_UNSET) 0L else positionMs.coerceAtLeast(0L)
        pendingPositionMs = target
        endReached = false
        // A load that is still opening seeks once it has, and an idle player starts the next load there.
        if (prepared && loaded) {
            sentPause = null
            session.post { MPVLib.command(arrayOf("seek", seconds(target), "absolute")) }
        }
        refresh()
        return DONE
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        this.playbackParameters = playbackParameters
        session.post { MPVLib.setPropertyDouble("speed", playbackParameters.speed.toDouble()) }
        return DONE
    }

    override fun handleSetVolume(volume: Float, volumeOperationType: Int): ListenableFuture<*> {
        this.volume = volume
        applyVolume()
        return DONE
    }

    override fun handleSetTrackSelectionParameters(parameters: TrackSelectionParameters): ListenableFuture<*> {
        trackSelectionParameters = parameters
        applyTrackChoices()
        return DONE
    }

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        output.bind(videoOutput)
        return DONE
    }

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        output.clear(videoOutput)
        return DONE
    }

    private fun playsNow(): Boolean =
        playWhenReady && holds.focusCommand == AudioFocusManager.PLAYER_COMMAND_PLAY_WHEN_READY

    /** Brings audio focus, the locks and mpv's pause in line with the state. */
    private fun refresh() {
        holds.update(playWhenReady, playbackState())
        applyPause()
    }

    private fun applyPause() {
        if (!prepared) return
        val pause = !playsNow()
        if (pause != sentPause) {
            sentPause = pause
            session.post { MPVLib.setPropertyBoolean("pause", pause) }
        }
    }

    private fun applyVolume() {
        val level = volume * holds.volumeMultiplier * MPV_FULL_VOLUME
        session.post { MPVLib.setPropertyDouble("volume", level.toDouble()) }
    }

    private fun applyTrackChoices() {
        if (!loaded) return
        val autoVideo = adaptiveVideoTrack(mpvTracks, bandwidthMeter.bitrateEstimate, bufferedMs - positionMs)
        val request = trackSelectionParameters.toMpvTrackRequest()
        val choices = mpvTrackChoices(request, mpvTracks, autoVideo)
        reloadForPickedVariant(request, choices[MpvTrackKind.Video])
        val changed = choices.filter { (kind, choice) -> (sentTrackChoices[kind] ?: MPV_DEFAULT_CHOICE) != choice }
        sentTrackChoices = choices
        if (changed.isEmpty()) return
        session.post { changed.forEach { (kind, choice) -> MPVLib.setPropertyString(kind.selectProperty, choice) } }
    }

    private fun onFileLoaded() {
        main.removeCallbacks(adaptQuality)
        main.postDelayed(adaptQuality, ADAPT_INTERVAL_MS)
        loaded = true
        everLoaded = true
        restarted = false
        firstFrameReported = false
        // A file's external tracks go with it, so every load adds them again. mpv opens each before it returns.
        val subtitles = externalSubtitles
        val audio = externalAudio
        session.post {
            // mpv chose the file's tracks before these were added, and adds them unselected. ExoPlayer chooses among
            // the stream's tracks and the merged external ones, so a stream with no audio of its own plays one of them.
            val selectAudio = !fileHasOwnAudio()
            subtitles.forEach { (_, url, subtitle) ->
                MPVLib.command(arrayOf("sub-add", url, "auto", subtitle.label, subtitle.language.orEmpty()))
            }
            audio.forEachIndexed { position, (_, url, track) ->
                val flag = if (selectAudio && position == 0) "select" else "auto"
                MPVLib.command(arrayOf("audio-add", url, flag, track.label, track.language.orEmpty()))
            }
        }
        applyTrackChoices()
        val target = pendingPositionMs
        if (target != null && target != loadStartMs) {
            session.post { MPVLib.command(arrayOf("seek", seconds(target), "absolute")) }
        }
    }

    private fun onPlaybackRestart() {
        restarted = true
        pendingPositionMs = null
        if (!firstFrameReported) {
            firstFrameReported = true
            newlyRenderedFirstFrame = true
        }
    }

    private fun onEndReached(reached: Boolean) {
        if (!reached) {
            endReached = false
            return
        }
        val duration = durationMs
        val position = positionMs
        if (duration != C.TIME_UNSET && duration - position > EARLY_END_TOLERANCE_MS) {
            fail(mpvEndedEarlyException(latestStreamFailure(), position, duration, recentErrors()))
        } else {
            endReached = true
        }
    }

    private fun onTracks(list: List<MpvTrack>) {
        mpvTracks = list
        tracks = media3Tracks()
        applyTrackChoices()
        val drawn = list.firstOrNull { it.kind == MpvTrackKind.Subtitle && it.selected }?.isDrawnByMpv == true
        if (drawn != mpvDrawsSubtitle) {
            mpvDrawsSubtitle = drawn
            if (drawn) cues = CueGroup.EMPTY_TIME_ZERO
            session.post { MPVLib.setPropertyBoolean("sub-visibility", drawn) }
        }
    }

    /**
     * Drops what mpv buffered ahead when a variant is picked in the set that plays, and loads again from the position,
     * as ExoPlayer does: the buffered segments came from the variants chosen before, and the pick would show only once
     * they had played. A change of track ([videoChoice]) reloads on its own, and Auto takes over from what is buffered.
     */
    private fun reloadForPickedVariant(request: MpvTrackRequest, videoChoice: String?) {
        val playing = mpvTracks.firstOrNull { it.kind == MpvTrackKind.Video && it.selected } ?: return
        // mpv keeps the track that plays for "auto".
        if ((videoChoice?.toIntOrNull() ?: playing.id) != playing.id) return
        val set = variantSets().firstOrNull { it.programId in playing.programIds } ?: return
        val pick = request.variantPick(set.programId, mpvTracks)
        val previous = appliedVariantPick
        appliedVariantPick = set.programId to pick
        if (previous?.first != set.programId || previous.second == pick || pick == HlsVariantPick.Auto) return
        session.post {
            val position = MPVLib.getPropertyDouble("time-pos") ?: return@post
            // A seek within what mpv buffered plays it from its cache: emptied, the seek loads the segment again.
            MPVLib.command(arrayOf("drop-buffers"))
            MPVLib.command(arrayOf("seek", position.toString(), "absolute+exact"))
        }
    }

    private fun media3Tracks(): Tracks =
        mpvMedia3Tracks(mpvTracks, externalSubtitles, externalAudio, variantSets(), positionMs)

    private fun onSubtitleText(text: String) {
        if (mpvDrawsSubtitle) return
        cues = if (text.isBlank()) {
            CueGroup.EMPTY_TIME_ZERO
        } else {
            CueGroup(listOf(Cue.Builder().setText(text).build()), Util.msToUs(positionMs))
        }
    }

    private fun fail(exception: PlaybackException) {
        error = exception
        prepared = false
        loaded = false
        logcat(LogPriority.ERROR, exception) { "mpv playback failed: ${exception.errorCodeName}" }
        // mpv would otherwise go on, to the next entry of its playlist or on with the stream, playing behind the
        // error with the position, which progress is saved from, still moving. Stopping also clears the playlist.
        session.post { MPVLib.command(arrayOf("stop")) }
    }

    /** Whether the loaded file has an audio track of its own. On the mpv thread. */
    private fun fileHasOwnAudio(): Boolean {
        val count = MPVLib.getPropertyInt("track-list/count") ?: 0
        return (0 until count).any { n ->
            MPVLib.getPropertyString("track-list/$n/type") == MpvTrackKind.Audio.mpvName &&
                MPVLib.getPropertyBoolean("track-list/$n/external") != true
        }
    }

    /** The proxy's last failure on the stream itself; a subtitle or audio track that didn't load doesn't stop playback. */
    private fun latestStreamFailure(): UpstreamFailure? {
        val trackUrls = (stream.subtitles.map { it.request.url } + stream.audioTracks.map { it.request.url }).toSet()
        return upstreamFailures.lastOrNull { it.url !in trackUrls }
    }

    private fun recentErrors(): List<String> = synchronized(errorLog) { errorLog.toList() }

    /** Applies [block] to the state on the main thread, unless the player is released by then. */
    private fun onMain(block: () -> Unit) {
        main.post {
            if (released) return@post
            block()
            refresh()
            invalidateState()
        }
    }

    private inner class Host : MpvSessionHost {
        override fun beforeCreate(): Boolean {
            if (!addon.loadNow()) return false
            val proxy = try {
                StreamProxy(
                    context = appContext,
                    client = client,
                    stream = stream,
                    headers = headers,
                    bandwidthMeter = bandwidthMeter,
                    bufferedAheadMs = { bufferedMs - positionMs },
                    variantPick = { set ->
                        trackSelectionParameters.toMpvTrackRequest().variantPick(set.programId, mpvTracks)
                    },
                )
            } catch (e: IOException) {
                logcat(LogPriority.ERROR, e) { "The mpv stream proxy didn't start" }
                return false
            }
            this@MpvPlayer.proxy = proxy
            val subtitles = stream.subtitles.mapIndexed { index, subtitle ->
                MpvExternalSubtitle(index, proxy.subtitleUrls[index], subtitle)
            }
            val audio = stream.audioTracks.mapIndexed { index, track ->
                MpvExternalAudio(index, proxy.audioUrls[index], track)
            }
            main.post {
                externalSubtitles = subtitles
                externalAudio = audio
                upstreamFailures = proxy.failures
                variantSets = { proxy.variantSets }
            }
            return true
        }

        override fun configure() {
            setWatchedPlayerOptions(mpvConfigDirectory(appContext))
        }

        override fun started() {
            observeWatchedPlayerProperties()
        }

        override fun failedToStart() {
            onMain {
                val failure = PlaybackException(
                    "mpv couldn't start: its add-on didn't load or its stream proxy didn't open",
                    null,
                    PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                )
                startFailure = failure
                fail(failure)
            }
        }

        override fun eventProperty(property: String) {
            when (property) {
                // mpv clears it when it stops, but a stopped or failed player keeps its timeline, as ExoPlayer's
                // does: progress is saved with it.
                "duration" -> onMain { if (prepared) durationMs = C.TIME_UNSET }
                "sub-text" -> onMain { onSubtitleText("") }
            }
        }

        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "dwidth" -> onMain { videoWidth = value.toInt() }
                "dheight" -> onMain { videoHeight = value.toInt() }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "paused-for-cache" -> onMain { pausedForCache = value }
                "seeking" -> onMain { seeking = value }
                "seekable" -> onMain { seekable = value }
                "eof-reached" -> onMain { onEndReached(value) }
            }
        }

        override fun eventProperty(property: String, value: String) {
            when (property) {
                "track-list" -> {
                    val list = try {
                        MpvTrack.parseList(value)
                    } catch (e: IllegalArgumentException) {
                        logcat(LogPriority.ERROR, e) { "mpv's track list didn't parse" }
                        return
                    }
                    onMain { onTracks(list) }
                }
                "sub-text" -> onMain { onSubtitleText(value) }
            }
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> positionMs = milliseconds(value)
                "demuxer-cache-time" -> bufferedMs = milliseconds(value)
                "duration" -> onMain { durationMs = milliseconds(value) }
            }
        }

        override fun event(eventId: Int) {
            when (eventId) {
                MpvEvent.MPV_EVENT_FILE_LOADED -> onMain(::onFileLoaded)
                MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                    // The event thread runs only while the instance exists, so it may read from it.
                    MPVLib.getPropertyDouble("time-pos")?.let { positionMs = milliseconds(it) }
                    onMain(::onPlaybackRestart)
                }
            }
        }

        override fun eventEndFile(endFile: MPVLib.EndFile) {
            if (endFile.playlistEntryId < entryId) return
            when (endFile.reason) {
                MpvEndFileReason.MPV_END_FILE_REASON_ERROR -> {
                    val log = recentErrors()
                    onMain {
                        fail(mpvPlaybackException(latestStreamFailure(), endFile.error, endFile.errorMessage, log))
                    }
                }
                MpvEndFileReason.MPV_END_FILE_REASON_EOF -> onMain { onEndReached(true) }
                // mpv read the stream as a playlist of its own, which Katari never hands it, and put its entries in
                // the stream's place: what it got wasn't media.
                MpvEndFileReason.MPV_END_FILE_REASON_REDIRECT -> {
                    val log = recentErrors()
                    onMain {
                        fail(
                            mpvPlaybackException(
                                latestStreamFailure(),
                                MpvError.MPV_ERROR_UNKNOWN_FORMAT,
                                "mpv read the stream as a playlist",
                                log,
                            ),
                        )
                    }
                }
            }
        }

        override fun logMessage(prefix: String, level: Int, text: String) {
            if (level > MpvLogLevel.MPV_LOG_LEVEL_ERROR) return
            synchronized(errorLog) {
                // The URLs mpv names are the proxy's, which carry the source's own path and tokens.
                errorLog.addLast("[$prefix] ${text.trim().replace(URL, "<url>")}")
                while (errorLog.size > ERROR_LOG_LINES) errorLog.removeFirst()
            }
        }
    }

    private inner class HoldsListener : WatchedPlaybackHolds.Listener {
        override fun onPauseRequested(reason: Int) {
            if (released) return
            playWhenReady = false
            playWhenReadyReason = reason
            refresh()
            invalidateState()
        }

        override fun onFocusChanged() {
            if (released) return
            applyVolume()
            applyPause()
            invalidateState()
        }
    }

    private companion object {
        val DONE: ListenableFuture<*> = Futures.immediateVoidFuture()
        const val MEDIA_ITEM_UID = "mpv"
        const val MPV_FULL_VOLUME = 100f
        const val MPV_DEFAULT_CHOICE = "auto"
        const val ERROR_LOG_LINES = 5
        const val ADAPT_INTERVAL_MS = 2_000L
        val URL = Regex("""\bhttps?://\S+""")

        /**
         * How far short of its duration a stream may end and still count as finished. Containers round their
         * durations, but a connection that dropped mid-episode ends minutes short.
         */
        const val EARLY_END_TOLERANCE_MS = 10_000L

        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val AVAILABLE_COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_RELEASE,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_SPEED_AND_PITCH,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_SET_VOLUME,
                Player.COMMAND_SET_VIDEO_SURFACE,
                Player.COMMAND_GET_TEXT,
                Player.COMMAND_GET_TRACKS,
                Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS,
            )
            .build()

        fun seconds(ms: Long): String = (ms / 1000.0).toString()

        fun milliseconds(seconds: Double): Long = (seconds * 1000).toLong()
    }
}
