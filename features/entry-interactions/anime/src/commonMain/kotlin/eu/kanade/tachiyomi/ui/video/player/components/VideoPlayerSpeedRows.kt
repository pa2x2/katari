package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.entry.interactions.player.PlaybackSpeed
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.control.StepperInput
import app.katari.presentation.kit.control.StepperKind
import app.katari.presentation.kit.list.ChipsRow
import app.katari.presentation.kit.list.StepperRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The preset speeds, and a stepper that reaches any speed between and beyond them, typed or a step at a time. A speed
 * between the presets selects none of them.
 */
@Composable
internal fun VideoPlayerSpeedRows(
    speed: PlaybackSpeed,
    onSelect: (PlaybackSpeed) -> Unit,
) {
    Column {
        ChipsRow(title = stringResource(MR.strings.anime_playback_speed)) {
            PlaybackSpeed.PRESETS.forEach { preset ->
                FilterChip(
                    label = preset.label,
                    selected = preset == speed,
                    onClick = { onSelect(preset) },
                )
            }
        }
        StepperRow(
            title = stringResource(MR.strings.anime_playback_speed_custom),
            value = speed.hundredths,
            valueRange = PlaybackSpeed.RANGE,
            step = PlaybackSpeed.STEP,
            kind = StepperKind.Amount,
            valueFormatter = { PlaybackSpeed(it).label },
            inputSuffix = "x",
            decreaseContentDescription = stringResource(MR.strings.anime_shortcut_slower),
            increaseContentDescription = stringResource(MR.strings.anime_shortcut_faster),
            editContentDescription = stringResource(MR.strings.anime_playback_speed_type),
            onValueChange = { onSelect(PlaybackSpeed(it)) },
            input = SpeedInput,
        )
    }
}

private val SpeedInput = StepperInput(
    text = { PlaybackSpeed(it).number },
    parse = { PlaybackSpeed.parse(it)?.hundredths },
    decimal = true,
)
