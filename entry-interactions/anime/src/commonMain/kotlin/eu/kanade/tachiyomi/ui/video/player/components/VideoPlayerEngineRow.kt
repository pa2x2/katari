package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.engine.EngineSwitchReason
import eu.kanade.tachiyomi.ui.video.player.engine.MpvTrackPick
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.domain.entry.model.PlayerEngineOverride
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * [autoReason] is why Auto plays where it does, shown while the title is on Auto. [trackPick] is what picking a track
 * marked mpv does.
 */
internal data class VideoPlayerEngineChoice(
    val override: PlayerEngineOverride?,
    val autoReason: EngineSwitchReason?,
    val mpvUsable: Boolean,
    val trackPick: MpvTrackPick,
)

/** The title's player: Auto, or one engine for every episode. */
@Composable
internal fun VideoPlayerEngineRow(
    choice: VideoPlayerEngineChoice,
    onSelect: (PlayerEngineOverride?) -> Unit,
) {
    val reason = choice.autoReason?.takeIf { choice.override == null }
    ChipsRow(
        title = stringResource(MR.strings.anime_player_engine),
        supportingText = when {
            reason != null -> stringResource(reason.line)
            !choice.mpvUsable -> stringResource(MR.strings.anime_player_engine_mpv_missing)
            else -> null
        },
    ) {
        FilterChip(
            label = stringResource(MR.strings.anime_player_engine_auto),
            selected = choice.override == null,
            onClick = { onSelect(null) },
        )
        FilterChip(
            label = stringResource(MR.strings.anime_player_engine_built_in),
            selected = choice.override == PlayerEngineOverride.BUILT_IN,
            onClick = { onSelect(PlayerEngineOverride.BUILT_IN) },
        )
        FilterChip(
            label = stringResource(MR.strings.anime_player_engine_mpv),
            selected = choice.override == PlayerEngineOverride.MPV,
            onClick = { onSelect(PlayerEngineOverride.MPV) },
            enabled = choice.mpvUsable,
        )
    }
}
