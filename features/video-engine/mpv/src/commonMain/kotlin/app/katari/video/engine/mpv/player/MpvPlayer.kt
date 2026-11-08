package app.katari.video.engine.mpv.player

import app.katari.video.engine.mpv.chapters.MpvChapter
import app.katari.video.engine.mpv.client.MpvClient
import app.katari.video.engine.mpv.client.MpvEndFile
import app.katari.video.engine.mpv.client.MpvEndReason
import app.katari.video.engine.mpv.client.MpvErrorCode
import app.katari.video.engine.mpv.client.MpvLogLevel
import app.katari.video.engine.mpv.client.MpvSessionHost
import app.katari.video.engine.mpv.client.MpvValue
import app.katari.video.engine.mpv.proxy.RelayLog
import app.katari.video.engine.mpv.proxy.StreamFailure
import app.katari.video.engine.mpv.proxy.StreamProxy
import app.katari.video.engine.mpv.proxy.hls.HlsVariantPick
import app.katari.video.engine.mpv.proxy.hls.HlsVariantSet
import app.katari.video.engine.mpv.quality.MpvBandwidthMeter
import app.katari.video.engine.mpv.quality.adaptiveVideoTrack
import app.katari.video.engine.mpv.quality.initialBitrateCap
import app.katari.video.engine.mpv.tracks.MpvExternalAudio
import app.katari.video.engine.mpv.tracks.MpvExternalSubtitle
import app.katari.video.engine.mpv.tracks.MpvTrack
import app.katari.video.engine.mpv.tracks.MpvTrackKind
import app.katari.video.engine.mpv.tracks.MpvTrackRequest
import app.katari.video.engine.mpv.tracks.mpvTrackChoices
import app.katari.video.engine.mpv.tracks.mpvVideoTracks
import app.katari.video.engine.spi.VideoPlayerRole
import app.katari.video.engine.spi.chapters.FileChapter
import app.katari.video.engine.spi.loading.VideoRequestHeaders
import app.katari.video.engine.spi.player.SubtitleCue
import app.katari.video.engine.spi.player.VideoOutput
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerCapabilities
import app.katari.video.engine.spi.player.VideoPlayerError
import app.katari.video.engine.spi.player.VideoPlayerEvent
import app.katari.video.engine.spi.player.VideoPlayerListener
import app.katari.video.engine.spi.player.VideoPlayerState
import app.katari.video.engine.spi.player.VideoSize
import app.katari.video.engine.spi.tracks.AudioTrackSelection
import app.katari.video.engine.spi.tracks.TextTrackSelection
import app.katari.video.engine.spi.tracks.VideoTrackSelection
import app.katari.video.engine.spi.tracks.VideoTracks
import eu.kanade.tachiyomi.source.entry.VideoStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import kotlin.math.log2

/**
 * A player on mpv, on every platform: one stream with its external subtitles and audio, through a [StreamProxy], on an
 * mpv instance the [platform] opens for it. It plays the watched player; the seek preview's silent one, which stays on
 * the variant it opened and never plays; and the immersive feed's clips, which read ahead little and may repeat. Only
 * the watched player holds anything outside itself.
 *
 * The state lives on the UI thread. mpv is driven from the session's mpv thread and reports on its event thread, whose
 * property changes come back here, except the playback position, which changes every frame and is read as it is.
 *
 * Subtitles mpv can't hand over as text, or that carry their own styling (ASS), mpv draws itself. Every other
 * subtitle is hidden in mpv and published as cues, which the app draws as it draws ExoPlayer's.
 */
internal class MpvPlayer(
    private val platform: MpvPlatform,
    private val client: OkHttpClient,
    private val stream: VideoStream,
    private val headers: VideoRequestHeaders,
    private val role: VideoPlayerRole,
    private val bandwidthMeter: MpvBandwidthMeter,
) : VideoPlayer {

    private val preview = role == VideoPlayerRole.SeekPreview

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = mutableListOf<VideoPlayerListener>()
    private val mutableState = MutableStateFlow(VideoPlayerState())
    private val pendingEvents = mutableListOf<VideoPlayerEvent>()

    // UI thread.
    private var released = false
    private var playWhenReady = false
    private var prepared = false
    private var everLoaded = false
    private var loaded = false
    private var restarted = false
    private var firstFrameReported = false
    private var pausedForCache = false
    private var seeking = false
    private var endReached = false
    private var error: VideoPlayerError? = null
    private var durationMs: Long? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var tracks = VideoTracks.Empty
    private var sentTrackChoices = emptyMap<MpvTrackKind, String>()
    private var adaptingQuality: Job? = null

    /** The playing set of variants and what was asked of it, since the file loaded. */
    private var appliedVariantPick: Pair<Int, HlsVariantPick>? = null

    private var speed = 1f
    private var volume = 1f
    private var volumeBoostPercent = VideoPlayer.NO_VOLUME_BOOST_PERCENT
    private var cues = emptyList<SubtitleCue>()
    private var mpvDrawsSubtitle = false
    private var fileChapters = emptyList<FileChapter>()
    private var sentPause: Boolean? = null
    private var externalSubtitles = emptyList<MpvExternalSubtitle>()
    private var externalAudio = emptyList<MpvExternalAudio>()
    private var streamFailure: () -> StreamFailure? = { null }
    private var variantSets: () -> List<HlsVariantSet> = { emptyList() }

    /** Why the instance couldn't start, which ends the session for good. */
    private var startFailure: VideoPlayerError? = null

    /** Where the current load started. */
    private var loadStartMs = 0L

    /** The position asked for that mpv hasn't reached yet: a seek, or the start of the next load. */
    private var pendingPositionMs: Long? = null

    // Written on the UI thread, read on the proxy's too: it asks what to play from a set of variants.
    @Volatile
    private var mpvTracks = emptyList<MpvTrack>()

    @Volatile
    private var trackRequest = MpvTrackRequest()

    // Written on mpv's threads, read anywhere.
    @Volatile
    private var positionMs = 0L

    @Volatile
    private var bufferedMs = 0L

    /** The playlist entry of the current load; the end of an earlier one is stale. */
    @Volatile
    private var entryId = -1L

    private val errorLog = MpvErrorLog()
    private val appLog = MpvAppLog(role)

    /** Confined to the mpv thread. */
    private var proxy: StreamProxy? = null

    private val holds = if (role is VideoPlayerRole.Watched) platform.holdPlayback(HoldsListener()) else NoPlaybackHolds

    // Last, because the mpv thread starts using the fields above as soon as the session opens.
    private val session = platform.openSession(Host())

    override val state: StateFlow<VideoPlayerState> = mutableState.asStateFlow()

    override val currentPositionMs: Long
        get() = pendingPositionMs ?: positionMs

    override val capabilities = VideoPlayerCapabilities(
        fileChapters = true,
        volumeBoost = true,
        drawsSubtitles = true,
        placesPicture = true,
        stepsFrames = true,
    )

    init {
        // The position changes every frame, which isn't worth a state each, so the state takes it on a ticker.
        scope.launch {
            while (isActive) {
                val moving = mutableState.value.run { isPlaying || playback == VideoPlayerState.Playback.Buffering }
                delay(if (moving) MOVING_POSITION_INTERVAL_MS else STILL_POSITION_INTERVAL_MS)
                publish()
            }
        }
    }

    override fun addListener(listener: VideoPlayerListener) {
        listeners += listener
    }

    override fun removeListener(listener: VideoPlayerListener) {
        listeners -= listener
    }

    override fun prepare() {
        // Nothing would ever load: the session is over.
        startFailure?.let {
            fail(it)
            publish()
            return
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
        session.post { client ->
            val proxy = proxy ?: return@post
            client.setFlag("pause", pause)
            client.setOption("start", seconds(start))
            // The seek preview opens the lowest variant: none is within no bitrate at all
            val bitrateCap = if (preview) 0L else initialBitrateCap(bandwidthMeter.bitrateEstimate)
            val options = loadfileOptions(streamFileOptions(stream, bitrateCap))
            // -1 is loadfile's playlist index, which "replace" ignores but mpv needs before the options.
            if (options == null) {
                client.command("loadfile", proxy.streamUrl, "replace")
            } else {
                client.command("loadfile", proxy.streamUrl, "replace", "-1", options)
            }
            client.getLong("playlist/0/id")?.let { entryId = it }
        }
        refresh()
        publish()
    }

    override fun play() = setPlayWhenReady(true)

    override fun pause() = setPlayWhenReady(false)

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        this.playWhenReady = playWhenReady
        holds.setPlayWhenReady(playWhenReady, playback())
        applyPause()
        publish()
    }

    override fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        pendingPositionMs = target
        endReached = false
        // A load that is still opening seeks once it has, and an idle player starts the next load there.
        if (prepared && loaded) {
            sentPause = null
            appLog.seekAsked(seconds(target))
            session.post { it.command("seek", seconds(target), "absolute") }
        }
        refresh()
        pendingEvents += VideoPlayerEvent.PositionJumped(target, bySeek = true)
        publish()
    }

    override fun seekBack() = seekBy(-seekStepMs)

    override fun seekForward() = seekBy(seekStepMs)

    // Stepping back seeks to the keyframe before and decodes up to the frame: slow where keyframes are far apart.
    override fun stepFrame(forward: Boolean) {
        if (!prepared || !loaded) return
        session.post { it.command(if (forward) "frame-step" else "frame-back-step") }
    }

    private val seekStepMs: Long get() = (role as? VideoPlayerRole.Watched)?.seekStepMs ?: 0L

    private fun seekBy(offsetMs: Long) {
        val target = currentPositionMs + offsetMs
        seekTo(durationMs?.let { target.coerceAtMost(it) } ?: target)
    }

    override fun stop() {
        prepared = false
        loaded = false
        endReached = false
        session.post { it.command("stop") }
        refresh()
        publish()
    }

    override fun release() {
        if (released) return
        released = true
        listeners.clear()
        scope.cancel()
        holds.release()
        session.close {
            proxy?.close()
            proxy = null
        }
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        session.post { it.setDouble("speed", speed.toDouble()) }
        publish()
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        applyVolume()
        publish()
    }

    override fun setVolumeBoost(percent: Int) {
        volumeBoostPercent = percent.coerceIn(VideoPlayer.NO_VOLUME_BOOST_PERCENT, VideoPlayer.MAX_VOLUME_BOOST_PERCENT)
        applyVolume()
    }

    // keep-open doesn't stop a looping file at its end, so a repeated clip never ends.
    override fun setRepeatOne(repeatOne: Boolean) {
        session.post { it.setString("loop-file", if (repeatOne) "inf" else "no") }
    }

    override fun selectVideo(selection: VideoTrackSelection) = select(trackRequest.with(selection, tracks))

    override fun selectAudio(selection: AudioTrackSelection) = select(trackRequest.with(selection, tracks))

    override fun selectText(selection: TextTrackSelection) = select(trackRequest.with(selection, tracks))

    private fun select(request: MpvTrackRequest) {
        trackRequest = request
        applyTrackChoices()
        publish()
    }

    override fun setVideoOutput(output: VideoOutput?) {
        if (released) return
        session.setVideoOutput(output)
    }

    // mpv's zoom is a power of two, and its pan is in the scaled picture's size, as asked; both outlast a reload.
    override fun placePicture(scale: Double, offsetX: Double, offsetY: Double) {
        session.post { client ->
            client.setDouble("video-zoom", log2(scale))
            client.setDouble("video-pan-x", offsetX)
            client.setDouble("video-pan-y", offsetY)
        }
    }

    private fun playback(): VideoPlayerState.Playback = when {
        error != null || !prepared -> VideoPlayerState.Playback.Idle
        endReached -> VideoPlayerState.Playback.Ended
        !loaded || !restarted || pausedForCache || seeking || pendingPositionMs != null ->
            VideoPlayerState.Playback.Buffering
        else -> VideoPlayerState.Playback.Ready
    }

    /** Hands the state to its observers, then the events that came with it to the listeners. */
    private fun publish() {
        if (released) return
        val playback = playback()
        val position = currentPositionMs
        mutableState.value = VideoPlayerState(
            positionMs = position,
            bufferedPositionMs = maxOf(bufferedMs, position),
            durationMs = durationMs,
            playback = playback,
            playWhenReady = playWhenReady,
            isPlaying = playWhenReady && playback == VideoPlayerState.Playback.Ready && holds.allowsPlaying,
            speed = speed,
            volume = volume,
            // mpv reports the size the picture is shown at, its pixels already made square.
            videoSize = if (videoWidth > 0 && videoHeight > 0) VideoSize(videoWidth, videoHeight, 1f) else null,
            tracks = tracks,
            cues = cues,
            drawsSelectedSubtitle = mpvDrawsSubtitle,
            fileChapters = fileChapters,
            error = error,
        )
        if (pendingEvents.isEmpty()) return
        val events = pendingEvents.toList()
        pendingEvents.clear()
        events.forEach { event -> listeners.toList().forEach { it.onEvent(event) } }
    }

    private fun playsNow(): Boolean = playWhenReady && holds.allowsPlaying

    /** Brings the holds and mpv's pause in line with the state. */
    private fun refresh() {
        holds.update(playWhenReady, playback())
        applyPause()
    }

    private fun applyPause() {
        if (!prepared) return
        val pause = !playsNow()
        if (pause != sentPause) {
            sentPause = pause
            session.post { it.setFlag("pause", pause) }
        }
    }

    private fun applyVolume() {
        val level = volume * holds.volumeMultiplier * volumeBoostPercent
        session.post { it.setDouble("volume", level.toDouble()) }
    }

    private fun applyTrackChoices() {
        if (!loaded) return
        val autoVideo = if (preview) {
            null
        } else {
            adaptiveVideoTrack(
                mpvTracks,
                bandwidthMeter.bitrateEstimate,
                bufferedMs - positionMs,
            )
        }
        val request = trackRequest
        val choices = mpvTrackChoices(request, mpvTracks, autoVideo)
        reloadForPickedVariant(request, choices[MpvTrackKind.Video])
        val changed = choices.filter { (kind, choice) -> (sentTrackChoices[kind] ?: MPV_DEFAULT_CHOICE) != choice }
        sentTrackChoices = choices
        if (changed.isEmpty()) return
        session.post { client -> changed.forEach { (kind, choice) -> client.setString(kind.selectProperty, choice) } }
    }

    /**
     * Auto's quality follows the network: the choice is weighed again while a file is loaded. Within a set of variants
     * the proxy chooses each segment's, and the tracks follow the variant that plays.
     */
    private fun adaptQuality() {
        adaptingQuality?.cancel()
        adaptingQuality = scope.launch {
            while (true) {
                delay(ADAPT_INTERVAL_MS)
                if (!loaded) return@launch
                applyTrackChoices()
                val updated = videoTracks()
                if (updated != tracks) {
                    tracks = updated
                    publish()
                }
            }
        }
    }

    private fun startLoadedFile() {
        if (!preview) adaptQuality()
        loaded = true
        everLoaded = true
        restarted = false
        firstFrameReported = false
        // A file's external tracks go with it, so every load adds them again. mpv opens each before it returns.
        val subtitles = externalSubtitles
        val audio = externalAudio
        session.post { client ->
            // mpv chose the file's tracks before these were added, and adds them unselected. ExoPlayer chooses among
            // the stream's tracks and the merged external ones, so a stream with no audio of its own plays one of them.
            val selectAudio = !client.fileHasOwnAudio()
            subtitles.forEach { (_, url, subtitle) ->
                client.command("sub-add", url, "auto", subtitle.label, subtitle.language.orEmpty())
            }
            audio.forEachIndexed { position, (_, url, track) ->
                val flag = if (selectAudio && position == 0) "select" else "auto"
                client.command("audio-add", url, flag, track.label, track.language.orEmpty())
            }
        }
        applyTrackChoices()
        val target = pendingPositionMs
        if (target != null && target != loadStartMs) {
            session.post { it.command("seek", seconds(target), "absolute") }
        }
    }

    /**
     * [atEnd] when mpv found nothing to play where it was asked to go. keep-open then shows the file's last frame, which
     * moves the position to the end before mpv reports reaching it, so the end is judged here, while the asked position
     * still stands: a failure keeps it as the position progress is saved at and a retry starts from.
     */
    private fun restartedPlayback(atEnd: Boolean) {
        if (!prepared) return
        if (atEnd) {
            onEndReached(true)
            if (!prepared) return
        }
        restarted = true
        pendingPositionMs = null
        if (!firstFrameReported) {
            firstFrameReported = true
            pendingEvents += VideoPlayerEvent.FirstFrameRendered
        }
    }

    /**
     * The end mpv reached is the stream's unless the stream's media stopped loading and nothing came after
     * ([StreamFailure.mediaServedSince]), or it came far short of the duration.
     */
    private fun onEndReached(reached: Boolean) {
        if (!reached) {
            endReached = false
            return
        }
        // A failed or stopped load has already ended.
        if (!prepared) return
        val failure = streamFailure()
        val duration = durationMs
        val position = positionMs
        when {
            failure != null && !failure.mediaServedSince -> fail(
                mpvEndedEarlyError(failure.failure, pendingPositionMs ?: position, duration, errorLog.recent()),
            )
            duration != null && duration - position > EARLY_END_TOLERANCE_MS ->
                fail(mpvEndedEarlyError(failure?.failure, position, duration, errorLog.recent()))
            else -> endReached = true
        }
    }

    private fun onTracks(list: List<MpvTrack>) {
        mpvTracks = list
        tracks = videoTracks()
        applyTrackChoices()
        val drawn = list.firstOrNull { it.kind == MpvTrackKind.Subtitle && it.selected }?.isDrawnByMpv == true
        if (drawn != mpvDrawsSubtitle) {
            mpvDrawsSubtitle = drawn
            if (drawn) cues = emptyList()
            session.post { it.setFlag("sub-visibility", drawn) }
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
        session.post { client ->
            val position = client.getDouble("time-pos") ?: return@post
            // A seek within what mpv buffered plays it from its cache: emptied, the seek loads the segment again.
            client.command("drop-buffers")
            client.command("seek", position.toString(), "absolute+exact")
        }
    }

    private fun videoTracks(): VideoTracks =
        mpvVideoTracks(mpvTracks, externalSubtitles, externalAudio, variantSets(), positionMs)

    private fun onSubtitleText(text: String) {
        if (mpvDrawsSubtitle) return
        cues = if (text.isBlank()) emptyList() else listOf(SubtitleCue(text))
    }

    private fun fail(error: VideoPlayerError) {
        this.error = error
        prepared = false
        loaded = false
        logcat(LogPriority.ERROR, error) { "mpv playback failed: ${error.codeName}" }
        // mpv would otherwise go on, to the next entry of its playlist or on with the stream, playing behind the
        // error with the position, which progress is saved from, still moving. Stopping also clears the playlist.
        session.post { it.command("stop") }
        pendingEvents += VideoPlayerEvent.Failed(error)
    }

    /** Whether the loaded file has an audio track of its own. On the mpv thread. */
    private fun MpvClient.fileHasOwnAudio(): Boolean {
        val count = getLong("track-list/count") ?: 0L
        return (0 until count).any { n ->
            getString("track-list/$n/type") == MpvTrackKind.Audio.mpvName && getFlag("track-list/$n/external") != true
        }
    }

    /** Applies [block] to the state on the UI thread, unless the player is released by then. */
    private fun onMain(block: () -> Unit) {
        scope.launch {
            if (released) return@launch
            block()
            refresh()
            publish()
        }
    }

    private inner class Host : MpvSessionHost {
        override fun beforeCreate(): Boolean {
            val proxy = try {
                StreamProxy(
                    localMedia = platform.localMedia,
                    client = client,
                    stream = stream,
                    headers = headers,
                    bandwidthMeter = bandwidthMeter,
                    bufferedAheadMs = { bufferedMs - positionMs },
                    variantPick = { set -> trackRequest.variantPick(set.programId, mpvTracks) },
                    relayLog = RelayLog(appLog.player, connectionBreaks = !preview),
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
            scope.launch {
                externalSubtitles = subtitles
                externalAudio = audio
                streamFailure = { proxy.latestStreamFailure }
                variantSets = { proxy.variantSets }
            }
            return true
        }

        override fun configure(client: MpvClient) {
            setWatchedPlayerOptions(client)
            when (role) {
                VideoPlayerRole.SeekPreview -> setSeekPreviewOptions(client)
                VideoPlayerRole.Feed -> setFeedOptions(client)
                is VideoPlayerRole.Watched -> Unit
            }
        }

        override fun started(client: MpvClient) {
            observeWatchedPlayerProperties(client)
        }

        override fun failedToStart() {
            onMain {
                val failure = mpvStartError()
                startFailure = failure
                fail(failure)
            }
        }

        override fun onPropertyChange(property: String, value: MpvValue) {
            when (value) {
                MpvValue.None -> when (property) {
                    // mpv clears it when it stops, but a stopped or failed player keeps its timeline, as ExoPlayer's
                    // does: progress is saved with it.
                    "duration" -> onMain { if (prepared) durationMs = null }
                    "sub-text" -> onMain { onSubtitleText("") }
                }
                is MpvValue.Int64 -> when (property) {
                    "dwidth" -> onMain { videoWidth = value.value.toInt() }
                    "dheight" -> onMain { videoHeight = value.value.toInt() }
                }
                is MpvValue.Flag -> when (property) {
                    "paused-for-cache" -> onMain { pausedForCache = value.value }
                    "seeking" -> onMain { seeking = value.value }
                    "eof-reached" -> onMain { onEndReached(value.value) }
                }
                is MpvValue.Text -> onTextProperty(property, value.value)
                is MpvValue.Double -> when (property) {
                    "time-pos" -> positionMs = milliseconds(value.value)
                    "demuxer-cache-time" -> bufferedMs = milliseconds(value.value)
                    "duration" -> onMain { durationMs = milliseconds(value.value) }
                }
            }
        }

        private fun onTextProperty(property: String, value: String) {
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
                "chapter-list" -> {
                    val list = try {
                        MpvChapter.parseList(value)
                    } catch (e: IllegalArgumentException) {
                        logcat(LogPriority.ERROR, e) { "mpv's chapter list didn't parse" }
                        return
                    }
                    val parsed = list.map(MpvChapter::toFileChapter).sortedBy(FileChapter::startMs)
                    onMain { fileChapters = parsed }
                }
                "sub-text" -> onMain { onSubtitleText(value) }
            }
        }

        override fun onFileLoaded() = onMain(::startLoadedFile)

        override fun onPlaybackRestart(client: MpvClient) {
            client.getDouble("time-pos")?.let { positionMs = milliseconds(it) }
            val atEnd = client.getFlag("eof-reached") == true
            onMain { restartedPlayback(atEnd) }
        }

        override fun onEndFile(end: MpvEndFile) {
            if (end.playlistEntryId < entryId) return
            when (end.reason) {
                MpvEndReason.Error -> {
                    val log = errorLog.recent()
                    onMain { fail(mpvPlaybackError(streamFailure()?.failure, end.error, end.errorMessage, log)) }
                }
                MpvEndReason.Eof -> onMain { onEndReached(true) }
                // mpv read the stream as a playlist of its own, which Katari never hands it, and put its entries in
                // the stream's place: what it got wasn't media.
                MpvEndReason.Redirect -> {
                    val log = errorLog.recent()
                    onMain {
                        fail(
                            mpvPlaybackError(
                                streamFailure()?.failure,
                                MpvErrorCode.UNKNOWN_FORMAT,
                                "mpv read the stream as a playlist",
                                log,
                            ),
                        )
                    }
                }
                MpvEndReason.Stop, MpvEndReason.Quit -> Unit
            }
        }

        override fun onLogMessage(prefix: String, level: MpvLogLevel, text: String) {
            errorLog.add(prefix, level, text)
            appLog.add(prefix, level, text)
        }
    }

    private inner class HoldsListener : WatchedPlaybackHolds.Listener {
        override fun onPauseRequested() {
            if (released) return
            playWhenReady = false
            refresh()
            publish()
        }

        override fun onHoldsChanged() {
            if (released) return
            applyVolume()
            applyPause()
            publish()
        }
    }

    private companion object {
        const val MPV_DEFAULT_CHOICE = "auto"
        const val ADAPT_INTERVAL_MS = 2_000L

        // As often as the timeline needs while the position moves; while it stands, only the buffered part grows.
        const val MOVING_POSITION_INTERVAL_MS = 250L
        const val STILL_POSITION_INTERVAL_MS = 750L

        /**
         * How far short of its duration a stream may end and still count as finished. Containers round their
         * durations, but a connection that dropped mid-episode ends minutes short.
         */
        const val EARLY_END_TOLERANCE_MS = 10_000L

        fun seconds(ms: Long): String = (ms / 1000.0).toString()

        fun milliseconds(seconds: Double): Long = (seconds * 1000).toLong()
    }
}
