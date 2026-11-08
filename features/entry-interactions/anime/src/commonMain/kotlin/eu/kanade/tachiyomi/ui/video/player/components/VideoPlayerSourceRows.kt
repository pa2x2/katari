package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.entry.interactions.anime.stream.streamChoiceKey
import app.katari.entry.interactions.anime.stream.streamDisplayLabel
import app.katari.presentation.kit.list.RowGroupNote
import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.ui.video.player.VideoPlaybackPreviewState
import eu.kanade.tachiyomi.ui.video.player.withSelectedDub
import eu.kanade.tachiyomi.ui.video.player.withSelectedServer
import eu.kanade.tachiyomi.ui.video.player.withSelectedSourceQuality
import eu.kanade.tachiyomi.ui.video.player.withSelectedStream
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The episode's dub, server, stream and source quality as a source lists them, for the playing stream's settings or
 * for a problem screen's picker. [listed] holds the options of [current], the selection playing or that failed;
 * [active] is the one a draft starts from, with the title's preferred source quality in it. [preview] holds the
 * options of a drafted dub or server, which the source lists separately. [failedServerKeys] are servers that failed
 * this episode.
 */
internal data class VideoSourceOptions(
    val listed: PlaybackDescriptor,
    val current: PlaybackSelection,
    val active: PlaybackSelection,
    val preview: VideoPlaybackPreviewState,
    val failedServerKeys: Set<String> = emptySet(),
)

/** The source rows to draw for [draft], and where they come from: the listed options or a drafted dub's preview. */
internal data class VideoSourceRows(
    val dubMatchesCurrent: Boolean,
    val matchesCurrent: Boolean,
    val changed: Boolean,
    val previewLoading: Boolean,
    val previewFailed: Boolean,
    /** The source answered the drafted dub or server with another one, so it can't be applied. */
    val previewNotOffered: Boolean,
    val dubs: List<VideoPlaybackOption>,
    val servers: List<VideoPlaybackOption>,
    val serversLoading: Boolean,
    val streams: List<VideoPlaybackOption>,
    val streamsEnabled: Boolean,
    val qualities: List<VideoPlaybackOption>,
    val qualitiesLoading: Boolean,
) {
    val showDubs: Boolean get() = dubs.isNotEmpty()
    private val previewUnlisted: Boolean get() = previewFailed || previewNotOffered
    val showServers: Boolean get() = serversLoading || servers.size > 1 || (previewUnlisted && !dubMatchesCurrent)
    val showStreams: Boolean get() = streams.size > 1
    val showQualities: Boolean
        get() = qualitiesLoading || qualities.isNotEmpty() || (previewUnlisted && dubMatchesCurrent && !matchesCurrent)
    val showsAny: Boolean get() = showDubs || showServers || showStreams || showQualities
}

/**
 * Servers are listed per dub and qualities per server, so a drafted dub or server shows the preview's, while it
 * loads or after it failed. Streams are the current server's and can't be picked across a drafted change.
 */
internal fun videoSourceRows(options: VideoSourceOptions, draft: PlaybackSelection): VideoSourceRows {
    val current = options.current
    val dubMatchesCurrent = draft.dubKey == current.dubKey
    val matchesCurrent = dubMatchesCurrent && draft.serverKey == current.serverKey
    val preview = options.preview.takeIf { !matchesCurrent && it.selection == draft }
    val previewLoading = preview?.isLoading == true
    val previewFailed = preview?.failed == true
    val previewNotOffered = preview?.notOffered == true
    val qualities = if (matchesCurrent) {
        options.listed.sourceQualities
    } else {
        options.preview.playbackData?.sourceQualities.orEmpty()
    }
    val servers = if (dubMatchesCurrent) options.listed.servers else options.preview.playbackData?.servers.orEmpty()
    return VideoSourceRows(
        dubMatchesCurrent = dubMatchesCurrent,
        matchesCurrent = matchesCurrent,
        changed = draft != options.active,
        previewLoading = previewLoading,
        previewFailed = previewFailed,
        previewNotOffered = previewNotOffered,
        dubs = options.listed.dubs,
        servers = servers,
        serversLoading = previewLoading && !dubMatchesCurrent && servers.isEmpty(),
        streams = options.listed.streams.map {
            VideoPlaybackOption(key = streamChoiceKey(it), label = streamDisplayLabel(it))
        },
        streamsEnabled = matchesCurrent && draft.sourceQualityKey == current.sourceQualityKey,
        qualities = qualities,
        qualitiesLoading = previewLoading && qualities.isEmpty(),
    )
}

/** Asks for a drafted dub's or server's options, which the rows show once they arrive. */
@Composable
internal fun VideoSourcePreviewEffect(
    options: VideoSourceOptions,
    draft: PlaybackSelection,
    onPreview: (PlaybackSelection) -> Unit,
) {
    LaunchedEffect(draft.dubKey, draft.serverKey) {
        val matchesCurrent = draft.dubKey == options.current.dubKey && draft.serverKey == options.current.serverKey
        onPreview(if (matchesCurrent) options.current else draft)
    }
}

/**
 * The dub, server, stream and source quality rows, each keyed by its section, and the Apply row once [rows] differ
 * from the active selection. [onDraft] gets each pick; [onRetryPreview] asks again for options that failed to load.
 * While [openingEpisodeName] opens over the playing episode the rows are dimmed under a note saying so.
 */
internal fun LazyListScope.videoSourceRowItems(
    rows: VideoSourceRows,
    draft: PlaybackSelection,
    options: VideoSourceOptions,
    onDraft: (PlaybackSelection) -> Unit,
    onRetryPreview: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    openingEpisodeName: String? = null,
) {
    val enabled = openingEpisodeName == null
    if (!enabled && rows.showsAny) {
        item(key = VideoSourceRowKeys.WAITING) {
            RowGroupNote(
                stringResource(MR.strings.anime_playback_source_waits_for_episode, openingEpisodeName.orEmpty()),
            )
        }
    }
    if (rows.showDubs) {
        item(key = VideoSourceRowKeys.DUB) {
            PlaybackOptionRow(
                options = rows.dubs,
                titleRes = MR.strings.anime_playback_dub,
                selectedKey = draft.dubKey,
                enabled = enabled,
                onSelect = { onDraft(draft.withSelectedDub(it, options.active)) },
            )
        }
    }
    if (rows.showServers) {
        item(key = VideoSourceRowKeys.SERVER) {
            when {
                rows.serversLoading -> LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_server)
                rows.previewNotOffered && !rows.dubMatchesCurrent -> UnlistedPlaybackOptionRow(
                    titleRes = MR.strings.anime_playback_server,
                    message = MR.strings.anime_playback_options_not_offered,
                )
                rows.previewFailed && !rows.dubMatchesCurrent -> UnlistedPlaybackOptionRow(
                    titleRes = MR.strings.anime_playback_server,
                    message = MR.strings.anime_playback_options_failed,
                    onRetry = onRetryPreview,
                )
                else -> PlaybackOptionRow(
                    options = rows.servers,
                    titleRes = MR.strings.anime_playback_server,
                    selectedKey = draft.serverKey,
                    enabled = enabled,
                    // Failures are the current dub's; another dub lists its own servers.
                    tags = stringResource(MR.strings.anime_playback_server_failed).let { failed ->
                        options.failedServerKeys.takeIf { rows.dubMatchesCurrent }.orEmpty().associateWith { failed }
                    },
                    onSelect = { onDraft(draft.withSelectedServer(it, options.active)) },
                )
            }
        }
    }
    if (rows.showStreams) {
        item(key = VideoSourceRowKeys.STREAM) {
            PlaybackOptionRow(
                options = rows.streams,
                titleRes = MR.strings.anime_playback_stream,
                selectedKey = draft.streamKey,
                enabled = enabled && rows.streamsEnabled,
                onSelect = { onDraft(draft.withSelectedStream(it)) },
            )
        }
    }
    if (rows.showQualities) {
        item(key = VideoSourceRowKeys.SOURCE_QUALITY) {
            when {
                rows.qualitiesLoading -> LoadingPlaybackOptionRow(titleRes = MR.strings.anime_playback_source_quality)
                // A drafted server's qualities; a drafted dub's failure shows on its server row.
                rows.previewNotOffered && rows.dubMatchesCurrent -> UnlistedPlaybackOptionRow(
                    titleRes = MR.strings.anime_playback_source_quality,
                    message = MR.strings.anime_playback_options_not_offered,
                )
                rows.previewFailed && rows.dubMatchesCurrent -> UnlistedPlaybackOptionRow(
                    titleRes = MR.strings.anime_playback_source_quality,
                    message = MR.strings.anime_playback_options_failed,
                    onRetry = onRetryPreview,
                )
                else -> PlaybackOptionRow(
                    options = rows.qualities,
                    titleRes = MR.strings.anime_playback_source_quality,
                    selectedKey = draft.sourceQualityKey,
                    enabled = enabled,
                    onSelect = { onDraft(draft.withSelectedSourceQuality(it, options.active)) },
                )
            }
        }
    }
    if (rows.changed && enabled) {
        item(key = VideoSourceRowKeys.APPLY) {
            ApplySourceRow(onCancel = onCancel, onApply = onApply, applyEnabled = !rows.previewNotOffered)
        }
    }
}

internal object VideoSourceRowKeys {
    const val DUB = "dub"
    const val SERVER = "server"
    const val STREAM = "stream"
    const val SOURCE_QUALITY = "source_quality"
    const val APPLY = "apply_source"
    const val WAITING = "source_waits_for_episode"
}
