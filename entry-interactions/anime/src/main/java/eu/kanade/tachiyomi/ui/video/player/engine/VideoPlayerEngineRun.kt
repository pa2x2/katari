package eu.kanade.tachiyomi.ui.video.player.engine

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.i18n.*

internal enum class VideoPlayerEngine {
    BuiltIn,
    Mpv,
    ;

    val other: VideoPlayerEngine
        get() = when (this) {
            BuiltIn -> Mpv
            Mpv -> BuiltIn
        }
}

internal val PlayerEngineOverride.engine: VideoPlayerEngine
    get() = when (this) {
        PlayerEngineOverride.BUILT_IN -> VideoPlayerEngine.BuiltIn
        PlayerEngineOverride.MPV -> VideoPlayerEngine.Mpv
    }

/**
 * Which engine plays the current stream and how it came to. Each switch starts a new player at the same position with
 * a new run; a new episode or a reloaded stream starts over from the title's choice.
 */
internal data class VideoPlayerEngineRun(
    val engine: VideoPlayerEngine,
    /** Why Auto moved to [engine]; null where the title's choice starts. */
    val reason: EngineSwitchReason? = null,
    /** The engines that couldn't play this stream, in the order they failed. */
    val failures: List<EngineFailure> = emptyList(),
    /** Plays this episode only, against the title's override, after "Try mpv" or "Try built-in". */
    val tryOnce: Boolean = false,
) {
    fun failed(engine: VideoPlayerEngine): Boolean = failures.any { it.engine == engine }
}

/** [details] is the technical text of the engine's error, for the problem screen's details. */
internal data class EngineFailure(val engine: VideoPlayerEngine, val details: String?)

/**
 * Why Auto plays in the engine it does. [line] goes under the Player row while it applies; [hint] is what the one hint
 * per title says when mpv would be better but its add-on is missing, for the reasons that let the built-in player play.
 */
internal enum class EngineSwitchReason(val line: StringResource, val hint: StringResource? = null) {
    BuiltInFailed(MR.strings.anime_player_engine_reason_built_in_failed),
    MpvFailed(MR.strings.anime_player_engine_reason_mpv_failed),
    UnsupportedVideo(MR.strings.anime_player_engine_reason_video, MR.strings.anime_player_engine_hint_video),
    UnsupportedAudio(MR.strings.anime_player_engine_reason_audio, MR.strings.anime_player_engine_hint_audio),
    UnsupportedSubtitles(
        MR.strings.anime_player_engine_reason_subtitles,
        MR.strings.anime_player_engine_hint_subtitles,
    ),
    StyledSubtitles(MR.strings.anime_player_engine_reason_styled, MR.strings.anime_player_engine_hint_styled),

    /**
     * The source asks for mpv. Katari's source API has no such hint yet: Aniyomi's `mpvArgs` arrives with a later
     * capability, which would pass this reason to [onMpvSignal] once the stream resolves.
     */
    SourceAsks(MR.strings.anime_player_engine_reason_source, MR.strings.anime_player_engine_hint_source),
}
