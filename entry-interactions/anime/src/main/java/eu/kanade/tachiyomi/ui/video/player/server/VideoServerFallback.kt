package eu.kanade.tachiyomi.ui.video.player.server

import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.VideoPlaybackOption
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.engine.EngineFailureOutcome
import eu.kanade.tachiyomi.ui.video.player.engine.EngineProblem

/*
 * Automatic server fallback while an episode opens. When the server it opens on fails before the first frame for a
 * reason another server could avoid (the source or the network, not decoding), the player moves on to the next server
 * the source lists. Each server is tried once, at most MAX_SERVER_ATTEMPTS per episode, and never after the viewer
 * picked a server: then the failure is theirs to see, with "Try another server" on the problem screen.
 */

internal data class ServerFallback(
    val tried: Set<String> = emptySet(),
    val explicitPick: Boolean = false,
) {
    /** The server after [failed] in the source's order that hasn't been tried, with [failed] marked as tried. */
    fun next(servers: List<VideoPlaybackOption>, failed: VideoPlaybackOption): Step? {
        if (explicitPick) return null
        val tried = tried + failed.key
        if (tried.size >= MAX_SERVER_ATTEMPTS) return null
        val start = servers.indexOfFirst { it.key == failed.key } + 1
        val next = (servers.drop(start) + servers.take(start)).firstOrNull { it.key !in tried } ?: return null
        return Step(ServerFallback(tried), failed = failed, next = next)
    }

    data class Step(val fallback: ServerFallback, val failed: VideoPlaybackOption, val next: VideoPlaybackOption)
}

/** The server [playbackData] played from: the selected one, else the first, which sources play by default. */
internal fun playingServer(playbackData: PlaybackDescriptor): VideoPlaybackOption? {
    return playbackData.servers.firstOrNull { it.key == playbackData.selection.serverKey }
        ?: playbackData.servers.firstOrNull()
}

/**
 * Whether another server could avoid [this]: the source or the network failed, or listed nothing Katari can play.
 */
internal val ResolveVideoStream.Reason.allowsServerFallback: Boolean
    get() = this is ResolveVideoStream.Reason.StreamFetchFailed ||
        this == ResolveVideoStream.Reason.StreamFetchTimeout ||
        this == ResolveVideoStream.Reason.NoStreams ||
        this == ResolveVideoStream.Reason.TorrentOnly

/**
 * Whether a player that failed before its first frame should move to another server: the stream couldn't be loaded
 * (Media3's 2xxx input/output errors), or neither engine could decode it.
 */
internal fun EngineFailureOutcome.allowsServerFallback(errorCode: Int): Boolean = when (this) {
    is EngineFailureOutcome.Switch -> false
    is EngineFailureOutcome.Show -> when (problem) {
        is EngineProblem.Playback -> errorCode in IO_ERROR_CODES
        is EngineProblem.BothFailed -> true
        is EngineProblem.OverrideFailed, is EngineProblem.NeedsAddon -> false
    }
}

/** What the player says after moving to another server by itself. */
internal data class ServerFallbackNotice(val failedServer: String, val playingServer: String)

private const val MAX_SERVER_ATTEMPTS = 3
private val IO_ERROR_CODES = 2000..2999
