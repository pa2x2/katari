package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.control.SegmentedButtons
import app.katari.presentation.kit.list.ControlRow
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

private val options = mapOf(
    ThemeMode.SYSTEM to MR.strings.theme_system,
    ThemeMode.LIGHT to MR.strings.theme_light,
    ThemeMode.DARK to MR.strings.theme_dark,
)

@Composable
fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
) {
    ControlRow {
        SegmentedButtons(
            options = options.keys.toList(),
            selected = value,
            onSelect = onItemClick,
            label = { stringResource(options.getValue(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
