package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.widget.preferenceHighlight
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.SegmentedButtons
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.tts.api.provider.TtsParameterRange
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

/** Pitch as three steps around the engine's default, which is all most voices can tell apart. */
@Composable
internal fun TtsPitchSetting(
    value: Float,
    range: TtsParameterRange,
    onChange: (Float) -> Unit,
) {
    val options = listOf(
        PitchOption(
            stringResource(MR.strings.tts_settings_pitch_low),
            (range.default * 0.8f).coerceAtLeast(range.minimum),
        ),
        PitchOption(stringResource(MR.strings.tts_settings_pitch_natural), range.default),
        PitchOption(
            stringResource(MR.strings.tts_settings_pitch_high),
            (range.default * 1.2f).coerceAtMost(range.maximum),
        ),
    ).distinctBy(PitchOption::value)
    val selected = options.minBy { abs(it.value - value) }

    Column(
        modifier = Modifier
            .preferenceHighlight()
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        Text(stringResource(MR.strings.tts_settings_pitch), role = TextRole.ItemTitle)
        SegmentedButtons(
            options = options,
            selected = selected,
            onSelect = { onChange(it.value) },
            label = { it.label },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private data class PitchOption(
    val label: String,
    val value: Float,
)
