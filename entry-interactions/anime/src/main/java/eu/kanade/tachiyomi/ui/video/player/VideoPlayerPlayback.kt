package eu.kanade.tachiyomi.ui.video.player

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import java.util.Locale

internal data class VideoPlayerPlaybackSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackEnded: Boolean = false,
)

internal enum class VideoPlayerSeekDirection {
    Backward,
    Forward,
}

internal data class VideoPlayerSeekFeedbackState(
    val direction: VideoPlayerSeekDirection,
    val totalSeconds: Int,
    val hidePlayerChrome: Boolean,
    val sequence: Long,
    val updatedAtMillis: Long,
)

internal data class VideoPlayerSeekPreviewState(
    val positionMs: Long = 0L,
    val visible: Boolean = false,
    val loading: Boolean = false,
    val available: Boolean = false,
)

internal enum class VideoPlayerSideGestureType {
    Brightness,
    Volume,
}

internal data class VideoPlayerSideGestureFeedbackState(
    val type: VideoPlayerSideGestureType,
    val level: Int,
    val maxLevel: Int = 100,
    val sequence: Long,
)

@OptIn(markerClass = [UnstableApi::class])
internal fun buildVideoPlayer(
    context: Context,
    networkHelper: NetworkHelper,
    mediaCache: VideoPlayerMediaCache,
    stream: VideoStream,
    subtitles: List<VideoSubtitle>,
): ExoPlayer {
    return ExoPlayer.Builder(
        context,
        buildVideoMediaSourceFactory(context, networkHelper, mediaCache, stream, subtitles),
    )
        .build()
        .apply {
            setMediaItem(stream.toMediaItem(subtitles))
        }
}

@OptIn(markerClass = [UnstableApi::class])
private fun buildVideoMediaSourceFactory(
    context: Context,
    networkHelper: NetworkHelper,
    mediaCache: VideoPlayerMediaCache,
    stream: VideoStream,
    subtitles: List<VideoSubtitle>,
): DefaultMediaSourceFactory {
    val requestHeaders = stream.request.headers
    val streamUri = Uri.parse(stream.request.url)
    val userAgent = requestHeaders.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
        ?: networkHelper.defaultUserAgentProvider()

    val okHttpDataSourceFactory = OkHttpDataSource.Factory(networkHelper.client)
        .setUserAgent(userAgent)
        .setDefaultRequestProperties(requestHeaders)
    val subtitleHeadersByUrl = subtitles.associate { it.request.url to it.request.headers }
    val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
    val resolvedDataSourceFactory = ResolvingDataSource.Factory(defaultDataSourceFactory) { dataSpec ->
        val resolvedUri = rewriteDownloadedHlsContentUri(baseUri = streamUri, candidateUri = dataSpec.uri)
        val resolvedDataSpec = if (resolvedUri == dataSpec.uri) {
            dataSpec
        } else {
            dataSpec.buildUpon().setUri(resolvedUri).build()
        }
        val headers = subtitleHeadersByUrl[resolvedUri.toString()].orEmpty()
        if (headers.isEmpty()) {
            resolvedDataSpec
        } else {
            resolvedDataSpec.withAdditionalHeaders(headers)
        }
    }
    val dataSourceFactory: DataSource.Factory = if (streamUri.scheme.equals("http", ignoreCase = true) ||
        streamUri.scheme.equals("https", ignoreCase = true)
    ) {
        CacheDataSource.Factory()
            .setCache(mediaCache.cache)
            .setUpstreamDataSourceFactory(resolvedDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    } else {
        resolvedDataSourceFactory
    }

    return DefaultMediaSourceFactory(dataSourceFactory)
}

internal fun VideoStream.toMediaItem(subtitles: List<VideoSubtitle>): MediaItem {
    return MediaItem.Builder()
        .setUri(request.url)
        .setMimeType(mimeType ?: type.toMimeType())
        .setSubtitleConfigurations(subtitles.map(VideoSubtitle::toMediaItemSubtitle))
        .build()
}

internal fun VideoSubtitle.toMediaItemSubtitle(): MediaItem.SubtitleConfiguration {
    val selectionFlags = buildSelectionFlags(isDefault = isDefault, isForced = isForced)
    val roleFlags = C.ROLE_FLAG_SUBTITLE
    return MediaItem.SubtitleConfiguration.Builder(Uri.parse(request.url))
        .setMimeType(mimeType ?: inferSubtitleMimeType(request.url))
        .setLanguage(language)
        .setSelectionFlags(selectionFlags)
        .setRoleFlags(roleFlags)
        .setLabel(label)
        .setId(subtitleChoiceKey(this))
        .build()
}

internal fun ExoPlayer.availableAdaptiveQualities(): List<VideoAdaptiveQualityOption> {
    val groups = currentTracks.groups
        .filter { it.getType() == C.TRACK_TYPE_VIDEO && it.isSupported() }
    if (groups.isEmpty()) return emptyList()

    val heights = groups
        .flatMap { group ->
            (0 until group.length)
                .filter { group.isTrackSupported(it) }
                .mapNotNull { index -> group.getTrackFormat(index).height.takeIf { it > 0 } }
        }
        .distinct()
        .sorted()

    return buildList {
        add(VideoAdaptiveQualityOption(label = "Auto", preference = VideoAdaptiveQualityPreference.Auto))
        heights.forEach { height ->
            add(
                VideoAdaptiveQualityOption(
                    label = "${height}p",
                    preference = VideoAdaptiveQualityPreference.SpecificHeight(height),
                ),
            )
        }
    }
}

internal fun ExoPlayer.capturePlaybackSnapshot(): VideoPlayerPlaybackSnapshot {
    val resolvedDurationMs = duration
        .takeIf { it > 0L && it != C.TIME_UNSET }
        ?: 0L
    val resolvedPositionMs = currentPosition
        .coerceAtLeast(0L)
        .coerceToPlaybackDuration(resolvedDurationMs)
    val resolvedBufferedPositionMs = bufferedPosition
        .coerceAtLeast(resolvedPositionMs)
        .coerceToPlaybackDuration(resolvedDurationMs)

    return VideoPlayerPlaybackSnapshot(
        positionMs = resolvedPositionMs,
        durationMs = resolvedDurationMs,
        bufferedPositionMs = resolvedBufferedPositionMs,
        isPlaying = isPlaying,
        isLoading = playbackState == Player.STATE_BUFFERING,
        playbackEnded = playbackState == Player.STATE_ENDED,
    )
}

internal fun formatPlaybackTimestamp(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L)
    val seconds = totalSeconds % 60L
    val totalMinutes = totalSeconds / 60L
    val minutes = totalMinutes % 60L
    val hours = totalMinutes / 60L

    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", totalMinutes, seconds)
    }
}

internal fun Long.coerceToPlaybackDuration(durationMs: Long): Long {
    return if (durationMs > 0L) {
        coerceIn(0L, durationMs)
    } else {
        coerceAtLeast(0L)
    }
}

internal fun ExoPlayer.applyAdaptiveQuality(preference: VideoAdaptiveQualityPreference) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setForceLowestBitrate(false)
        .setForceHighestSupportedBitrate(false)

    when (preference) {
        VideoAdaptiveQualityPreference.Auto -> {
            builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        }
        is VideoAdaptiveQualityPreference.SpecificHeight -> {
            val override = currentTracks.groups
                .asSequence()
                .filter { it.getType() == C.TRACK_TYPE_VIDEO && it.isSupported() }
                .mapNotNull { group -> preferredTrackOverride(group, preference.height) }
                .minByOrNull { it.first }
                ?.second

            if (override != null) {
                builder.setOverrideForType(override)
            } else {
                builder.setMaxVideoSize(Int.MAX_VALUE, preference.height)
            }
        }
    }

    trackSelectionParameters = builder.build()
}

internal fun ExoPlayer.availableSubtitleTracks(
    externalSubtitles: List<VideoSubtitle>,
): List<VideoPlayerSubtitleOption> {
    val externalSubtitleKeys = externalSubtitles.mapTo(mutableSetOf(), ::subtitleChoiceKey)
    val externalSubtitleFingerprints = externalSubtitles.mapTo(mutableSetOf(), ::subtitleTrackFingerprint)
    val options = externalSubtitleOptions(externalSubtitles).toMutableList()

    currentTracks.groups.forEachIndexed { groupIndex, group ->
        if (group.getType() != C.TRACK_TYPE_TEXT || !group.isSupported()) return@forEachIndexed
        repeat(group.length) { trackIndex ->
            if (!group.isTrackSupported(trackIndex)) return@repeat
            val format = group.getTrackFormat(trackIndex)
            if (
                format.id in externalSubtitleKeys ||
                format.subtitleTrackFingerprint() in externalSubtitleFingerprints ||
                !shouldExposeEmbeddedSubtitle(format)
            ) {
                return@repeat
            }
            val subtitleSelection = embeddedSubtitleSelection(groupIndex, trackIndex, group)
            options += VideoPlayerSubtitleOption(
                key = subtitleSelection.key,
                label = subtitleSelection.label,
                selection = subtitleSelection,
            )
        }
    }

    return options.distinctBy(VideoPlayerSubtitleOption::key)
}

internal fun ExoPlayer.resolveAppliedSubtitleSelection(
    requested: VideoPlayerSubtitleSelection,
    externalSubtitles: List<VideoSubtitle>,
): VideoPlayerSubtitleSelection {
    return when (requested) {
        VideoPlayerSubtitleSelection.None -> requested
        VideoPlayerSubtitleSelection.Default -> {
            if (externalSubtitles.isEmpty()) {
                currentTracks.groups
                    .firstNotNullOfOrNull { group ->
                        if (group.getType() != C.TRACK_TYPE_TEXT || !group.isSupported()) {
                            return@firstNotNullOfOrNull null
                        }
                        (0 until group.length)
                            .firstOrNull { index ->
                                group.isTrackSupported(index) &&
                                    group.getTrackFormat(index).selectionFlags and C.SELECTION_FLAG_DEFAULT != 0
                            }
                            ?.let { index -> group to index }
                    }
                    ?.let { (group, index) ->
                        currentTracks.groups.indexOf(group).takeIf { it >= 0 }?.let { groupIndex ->
                            embeddedSubtitleSelection(groupIndex, index, group)
                        }
                    }
                    ?: VideoPlayerSubtitleSelection.None
            } else {
                val defaultSubtitle = externalSubtitles.firstOrNull(VideoSubtitle::isDefault)
                    ?: externalSubtitles.firstOrNull()
                defaultSubtitle?.let { subtitle ->
                    if (matchingTextTrack(subtitle) != null) {
                        VideoPlayerSubtitleSelection.External(subtitle)
                    } else {
                        VideoPlayerSubtitleSelection.Default
                    }
                } ?: VideoPlayerSubtitleSelection.None
            }
        }
        is VideoPlayerSubtitleSelection.External -> {
            val selectedExternalSubtitle = externalSubtitles.firstOrNull {
                subtitleChoiceKey(it) == subtitleChoiceKey(requested.subtitle)
            } ?: return VideoPlayerSubtitleSelection.None
            if (matchingTextTrack(selectedExternalSubtitle) != null) {
                VideoPlayerSubtitleSelection.External(selectedExternalSubtitle)
            } else {
                VideoPlayerSubtitleSelection.External(selectedExternalSubtitle)
            }
        }
        is VideoPlayerSubtitleSelection.Embedded -> {
            matchingEmbeddedTextTrack(requested)
                ?.let { match -> embeddedSubtitleSelection(match.groupIndex, match.trackIndex, match.group) }
                ?: requested
        }
    }
}

internal fun ExoPlayer.applySubtitleSelection(selection: VideoPlayerSubtitleSelection) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    when (selection) {
        VideoPlayerSubtitleSelection.None -> {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            builder.setPreferredTextLanguage(null)
        }
        VideoPlayerSubtitleSelection.Default,
        -> {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            builder.setPreferredTextLanguage(null)
        }
        is VideoPlayerSubtitleSelection.External -> {
            val override = matchingTextTrack(selection.subtitle)
            if (override == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                builder.setPreferredTextLanguage(null)
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setPreferredTextLanguage(null)
                builder.setOverrideForType(override)
            }
        }
        is VideoPlayerSubtitleSelection.Embedded -> {
            val match = matchingEmbeddedTextTrack(selection)
            if (match == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                builder.setPreferredTextLanguage(null)
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                builder.setPreferredTextLanguage(null)
                builder.setOverrideForType(TrackSelectionOverride(match.group.getMediaTrackGroup(), match.trackIndex))
            }
        }
    }

    trackSelectionParameters = builder.build()
}

internal fun PlayerView.applySubtitleAppearance(
    appearance: VideoSubtitleAppearance,
    editorVisible: Boolean = false,
) {
    val subtitleView = getSubtitleView() ?: return
    subtitleView.applyAppearance(appearance)
    subtitleView.visibility = if (editorVisible) android.view.View.INVISIBLE else android.view.View.VISIBLE
}

internal fun SubtitleView.applyAppearance(appearance: VideoSubtitleAppearance) {
    val normalizedAppearance = appearance.normalized()
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setViewType(SubtitleView.VIEW_TYPE_CANVAS)
    setBottomPaddingFraction(DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION)
    setStyle(
        CaptionStyleCompat(
            normalizedAppearance.textColor,
            withAlpha(normalizedAppearance.backgroundColor, normalizedAppearance.backgroundOpacity),
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            Color.BLACK,
            null,
        ),
    )
    setFractionalTextSize(normalizedAppearance.textSize)
    post {
        translationX = width * normalizedAppearance.offsetX
        translationY = height * normalizedAppearance.offsetY
    }
}

internal fun Player.subtitlePreviewText(): String? {
    val cues = currentCues.cues
    return cues.firstNotNullOfOrNull { cue -> cue.text?.toString()?.trim()?.takeIf(String::isNotBlank) }
}

internal fun Player.subtitlePreviewCues(): List<Cue> {
    return currentCues.cues.filter { cue ->
        cue.text?.toString()?.trim()?.isNotBlank() == true
    }
}

internal fun withAlpha(color: Int, opacity: Float): Int {
    val alpha = (opacity.coerceIn(0f, 1f) * 255f).toInt()
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

private fun preferredTrackOverride(group: Tracks.Group, targetHeight: Int): Pair<Int, TrackSelectionOverride>? {
    val candidate = (0 until group.length)
        .filter { group.isTrackSupported(it) }
        .mapNotNull { index ->
            val height = group.getTrackFormat(index).height.takeIf { it > 0 } ?: return@mapNotNull null
            index to height
        }
        .minByOrNull { (_, height) -> kotlin.math.abs(height - targetHeight) }
        ?: return null

    val trackIndex = candidate.first
    val heightDistance = kotlin.math.abs(candidate.second - targetHeight)
    return heightDistance to TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex)
}

private fun VideoStreamType.toMimeType(): String? {
    return when (this) {
        VideoStreamType.HLS -> MimeTypes.APPLICATION_M3U8
        VideoStreamType.DASH -> MimeTypes.APPLICATION_MPD
        VideoStreamType.PROGRESSIVE -> MimeTypes.VIDEO_MP4
        VideoStreamType.UNKNOWN -> null
    }
}

private fun inferSubtitleMimeType(url: String): String? {
    return when {
        url.endsWith(".vtt", ignoreCase = true) || url.endsWith(".webvtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
        url.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
        url.endsWith(".ass", ignoreCase = true) || url.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
        else -> null
    }
}

private fun rewriteDownloadedHlsContentUri(baseUri: Uri, candidateUri: Uri): Uri {
    if (baseUri.scheme != "content" || candidateUri.scheme != "content") return candidateUri

    val baseDocumentId = runCatching { DocumentsContract.getDocumentId(baseUri) }.getOrNull() ?: return candidateUri
    val candidateDocumentId =
        runCatching { DocumentsContract.getDocumentId(candidateUri) }.getOrNull() ?: return candidateUri
    val baseParentDocumentId = baseDocumentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (baseParentDocumentId.isBlank() || candidateDocumentId.startsWith("$baseParentDocumentId/")) {
        return candidateUri
    }

    return DocumentsContract.buildDocumentUriUsingTree(
        baseUri,
        "$baseParentDocumentId/$candidateDocumentId",
    )
}

private fun legacyEmbeddedSubtitleChoiceKey(groupIndex: Int, trackIndex: Int): String {
    return "embedded:$groupIndex:$trackIndex"
}

private fun embeddedSubtitleChoiceKey(format: Format): String {
    return "embedded:${format.subtitleTrackFingerprint()}"
}

private fun ExoPlayer.matchingEmbeddedTextTrack(
    selection: VideoPlayerSubtitleSelection.Embedded,
): EmbeddedTextTrackMatch? {
    return currentTracks.groups.withIndex()
        .asSequence()
        .filter { (_, group) -> group.getType() == C.TRACK_TYPE_TEXT && group.isSupported() }
        .mapNotNull { (groupIndex, group) ->
            (0 until group.length)
                .firstOrNull { trackIndex ->
                    if (!group.isTrackSupported(trackIndex)) {
                        return@firstOrNull false
                    }
                    val format = group.getTrackFormat(trackIndex)
                    embeddedSubtitleChoiceKey(format) == selection.key ||
                        legacyEmbeddedSubtitleChoiceKey(groupIndex, trackIndex) == selection.key
                }
                ?.let { trackIndex -> EmbeddedTextTrackMatch(groupIndex, group, trackIndex) }
        }
        .firstOrNull()
}

private fun ExoPlayer.matchingTextTrack(subtitle: VideoSubtitle): TrackSelectionOverride? {
    val subtitleKey = subtitleChoiceKey(subtitle)
    return currentTracks.groups.asSequence()
        .filter { it.getType() == C.TRACK_TYPE_TEXT && it.isSupported() }
        .mapNotNull { group ->
            (0 until group.length)
                .firstOrNull { trackIndex ->
                    if (!group.isTrackSupported(trackIndex)) {
                        return@firstOrNull false
                    }
                    val format = group.getTrackFormat(trackIndex)
                    val strippedId = format.id?.substringAfter(':') ?: format.id
                    strippedId == subtitleKey
                }
                ?.let { trackIndex -> TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex) }
        }
        .firstOrNull()
}

private fun shouldExposeEmbeddedSubtitle(format: Format): Boolean {
    val label = format.label?.trim().orEmpty()
    val language = format.language?.trim().orEmpty()
    val hasMeaningfulLabel = label.isNotBlank() && !label.equals("subtitle", ignoreCase = true)
    val hasMeaningfulLanguage = language.isNotBlank() && !language.equals("und", ignoreCase = true)
    val hasFlags = format.selectionFlags and (C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED) != 0
    return hasMeaningfulLabel || hasMeaningfulLanguage || hasFlags
}

private fun embeddedSubtitleSelection(
    groupIndex: Int,
    trackIndex: Int,
    group: Tracks.Group,
): VideoPlayerSubtitleSelection.Embedded {
    val format = group.getTrackFormat(trackIndex)
    return VideoPlayerSubtitleSelection.Embedded(
        groupIndex = groupIndex,
        trackIndex = trackIndex,
        key = embeddedSubtitleChoiceKey(format),
        label = buildEmbeddedSubtitleLabel(
            label = format.label,
            language = format.language,
            isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
            isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
        ),
        language = format.language,
        isDefault = format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
        isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
    )
}

private data class EmbeddedTextTrackMatch(
    val groupIndex: Int,
    val group: Tracks.Group,
    val trackIndex: Int,
)

private fun buildEmbeddedSubtitleLabel(
    label: String?,
    language: String?,
    isDefault: Boolean,
    isForced: Boolean,
): String {
    val base = label?.takeIf(String::isNotBlank)
        ?: language?.takeIf(String::isNotBlank)
        ?: "Subtitle"
    val suffixes = buildList {
        if (isDefault) add("Default")
        if (isForced) add("Forced")
    }
    return if (suffixes.isEmpty()) {
        base
    } else {
        "$base (${suffixes.joinToString()})"
    }
}

private fun subtitleTrackFingerprint(subtitle: VideoSubtitle): String {
    return listOf(
        subtitle.label.trim().lowercase(Locale.ROOT),
        subtitle.language?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        subtitle.isDefault.toString(),
        subtitle.isForced.toString(),
    ).joinToString("|")
}

private fun Format.subtitleTrackFingerprint(): String {
    return listOf(
        label?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        language?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        (selectionFlags and C.SELECTION_FLAG_DEFAULT != 0).toString(),
        (selectionFlags and C.SELECTION_FLAG_FORCED != 0).toString(),
    ).joinToString("|")
}

private fun buildSelectionFlags(isDefault: Boolean, isForced: Boolean): Int {
    var flags = 0
    if (isDefault) {
        flags = flags or C.SELECTION_FLAG_DEFAULT
    }
    if (isForced) {
        flags = flags or C.SELECTION_FLAG_FORCED
    }
    return flags
}

internal const val OFF_SUBTITLE_KEY = "off"
internal const val DEFAULT_SUBTITLE_KEY = "default"

internal fun subtitleChoiceKey(subtitle: VideoSubtitle): String {
    return subtitle.key.ifBlank {
        listOf(subtitle.label, subtitle.language, subtitle.request.url).joinToString("|")
    }
}
