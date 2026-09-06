package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import mihon.entry.interactions.reader.settings.ReadingMode
import tachiyomi.presentation.core.components.reader.ReaderReadingModeDialog

private val ReadingModesWithoutDefault = ReadingMode.entries - ReadingMode.DEFAULT

@Composable
internal fun ReadingModeSelectDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    onChange: (StringResource) -> Unit,
) {
    val readingMode by screenModel.readingModeFlow.collectAsState()
    val applyMode: (ReadingMode) -> Unit = {
        screenModel.onChangeReadingMode(it)
        onChange(it.stringRes)
        onDismissRequest()
    }
    ReaderReadingModeDialog(
        modes = ReadingModesWithoutDefault,
        currentMode = readingMode,
        modeLabel = ReadingMode::stringRes,
        modeIcon = ReadingMode::iconRes,
        onApply = applyMode,
        onUseDefault = { applyMode(ReadingMode.DEFAULT) }.takeIf { readingMode != ReadingMode.DEFAULT },
        onDismissRequest = onDismissRequest,
    )
}

@PreviewLightDark
@Composable
private fun ReadingModeDialogPreview() {
    TachiyomiPreviewTheme {
        ReaderReadingModeDialog(
            modes = ReadingModesWithoutDefault,
            currentMode = ReadingMode.LEFT_TO_RIGHT,
            modeLabel = ReadingMode::stringRes,
            modeIcon = ReadingMode::iconRes,
            onApply = {},
            onUseDefault = {},
            onDismissRequest = {},
        )
    }
}
