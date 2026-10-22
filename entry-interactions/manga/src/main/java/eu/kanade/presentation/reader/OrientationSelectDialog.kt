package eu.kanade.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.viewer.chrome.ui.ModeSelectionSheet
import mihon.presentation.kit.list.IconChoiceGrid
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

private val ReaderOrientationsWithoutDefault = ReaderOrientation.entries - ReaderOrientation.DEFAULT

@Composable
internal fun OrientationSelectDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    onChange: (StringResource) -> Unit,
) {
    val orientation by screenModel.orientationFlow.collectAsState()

    var selected by remember(orientation) { mutableStateOf(orientation) }
    val onChangeOrientation: (ReaderOrientation) -> Unit = {
        screenModel.onChangeOrientation(it)
        onChange(it.stringRes)
        onDismissRequest()
    }
    ModeSelectionSheet(
        title = stringResource(MR.strings.rotation_type),
        onDismissRequest = onDismissRequest,
        onApply = { onChangeOrientation(selected) },
        onUseDefault = { onChangeOrientation(ReaderOrientation.DEFAULT) }
            .takeIf { orientation != ReaderOrientation.DEFAULT },
    ) {
        IconChoiceGrid(
            options = ReaderOrientationsWithoutDefault,
            selected = selected,
            onSelect = { selected = it },
            icon = { it.icon },
            label = { stringResource(it.stringRes) },
            wideTiles = true,
        )
    }
}
