package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.components.SettingsIconGrid
import tachiyomi.presentation.core.components.material.IconToggleButton
import tachiyomi.presentation.core.i18n.stringResource

/** Shared reading-mode picker. Readers supply choices and persistence, never dialog layout. */
@Composable
fun <T : Any> ReaderReadingModeDialog(
    modes: List<T>,
    currentMode: T,
    modeLabel: (T) -> StringResource,
    modeIcon: (T) -> Int,
    onApply: (T) -> Unit,
    onUseDefault: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    var selectedIndex by rememberSaveable(currentMode) { mutableIntStateOf(modes.indexOf(currentMode)) }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        AdaptiveSheet(
            isTabletUi = LocalConfiguration.current.smallestScreenWidthDp >= 720,
            enableImplicitDismiss = true,
            onDismissRequest = onDismissRequest,
        ) {
            ModeSelectionDialog(
                onApply = { onApply(modes.getOrNull(selectedIndex) ?: currentMode) },
                onUseDefault = onUseDefault,
            ) {
                SettingsIconGrid(MR.strings.pref_category_reading_mode, columns = GridCells.Adaptive(200.dp)) {
                    itemsIndexed(modes) { index, mode ->
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            IconToggleButton(
                                checked = index == selectedIndex,
                                onCheckedChange = { selectedIndex = index },
                                modifier = Modifier.fillMaxWidth(),
                                imageVector = ImageVector.vectorResource(modeIcon(mode)),
                                title = stringResource(modeLabel(mode)),
                            )
                        }
                    }
                }
            }
        }
    }
}
