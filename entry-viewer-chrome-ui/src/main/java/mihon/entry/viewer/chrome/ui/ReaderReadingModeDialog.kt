package mihon.entry.viewer.chrome.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import mihon.presentation.kit.list.IconChoiceGrid
import mihon.presentation.kit.sheet.Sheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shared reading-mode picker. Readers supply choices and persistence, never dialog layout. */
@Composable
fun <T : Any> ReaderReadingModeDialog(
    modes: List<T>,
    currentMode: T,
    modeLabel: (T) -> StringResource,
    modeIcon: (T) -> ImageVector,
    onApply: (T) -> Unit,
    onUseDefault: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    var selectedIndex by rememberSaveable(currentMode) { mutableIntStateOf(modes.indexOf(currentMode)) }
    Sheet(
        onDismissRequest = onDismissRequest,
    ) {
        ModeSelectionDialog(
            onApply = { onApply(modes.getOrNull(selectedIndex) ?: currentMode) },
            onUseDefault = onUseDefault,
        ) {
            IconChoiceGrid(
                title = stringResource(MR.strings.pref_category_reading_mode),
                options = modes,
                selected = modes.getOrNull(selectedIndex) ?: currentMode,
                onSelect = { selectedIndex = modes.indexOf(it) },
                icon = modeIcon,
                label = { stringResource(modeLabel(it)) },
                wideTiles = true,
            )
        }
    }
}
