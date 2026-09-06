package mihon.entry.interactions.book.document.reader.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinding
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBarAction
import tachiyomi.presentation.core.components.reader.ReaderReadingModeDialog
import tachiyomi.presentation.core.i18n.stringResource

/** Chrome entry point for changing this book's mode or returning to the profile default. */
@Composable
internal fun RowScope.BookDocumentReadingModeShortcut(binding: ViewerSettingBinding<BookDocumentReadingMode>) {
    val mode by binding.state.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(mode.effectiveValue.stringRes)
    val scope = rememberCoroutineScope()
    ReaderChromeBottomBarAction(
        onClick = { open = true },
        modifier = Modifier.semantics { stateDescription = description },
    ) {
        Icon(
            painter = painterResource(mode.effectiveValue.iconRes),
            contentDescription = stringResource(MR.strings.viewer),
        )
    }
    if (open) {
        val useDefault: () -> Unit = {
            scope.launch {
                binding.clearEntryOverride()
                open = false
            }
        }
        ReaderReadingModeDialog(
            modes = BookDocumentReadingMode.entries,
            currentMode = mode.effectiveValue,
            modeLabel = BookDocumentReadingMode::stringRes,
            modeIcon = BookDocumentReadingMode::iconRes,
            onApply = { selected ->
                scope.launch {
                    val current = binding.state.value
                    if (selected == (current.profileValue ?: current.processorDefault)) {
                        binding.clearEntryOverride()
                    } else {
                        binding.setEntryOverride(selected)
                    }
                    open = false
                }
            },
            onUseDefault = useDefault.takeIf { mode.entryOverride != null },
            onDismissRequest = { open = false },
        )
    }
}
