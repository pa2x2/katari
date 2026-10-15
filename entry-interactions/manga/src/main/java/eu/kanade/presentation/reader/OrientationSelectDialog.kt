package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.viewer.chrome.ui.ModeSelectionDialog
import mihon.presentation.kit.list.IconChoiceGrid
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme
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

    Sheet(onDismissRequest = onDismissRequest) {
        DialogContent(
            orientation = orientation,
            onChangeOrientation = {
                screenModel.onChangeOrientation(it)
                onChange(it.stringRes)
                onDismissRequest()
            },
        )
    }
}

@Composable
private fun DialogContent(
    orientation: ReaderOrientation,
    onChangeOrientation: (ReaderOrientation) -> Unit,
) {
    var selected by remember { mutableStateOf(orientation) }

    ModeSelectionDialog(
        onUseDefault = {
            onChangeOrientation(
                ReaderOrientation.DEFAULT,
            )
        }.takeIf { orientation != ReaderOrientation.DEFAULT },
        onApply = { onChangeOrientation(selected) },
    ) {
        IconChoiceGrid(
            title = stringResource(MR.strings.rotation_type),
            options = ReaderOrientationsWithoutDefault,
            selected = selected,
            onSelect = { selected = it },
            icon = { it.icon },
            label = { stringResource(it.stringRes) },
        )
    }
}

@PreviewLightDark
@Composable
private fun DialogContentPreview() {
    KatariPreviewTheme {
        Surface {
            Column {
                DialogContent(
                    orientation = ReaderOrientation.DEFAULT,
                    onChangeOrientation = {},
                )

                DialogContent(
                    orientation = ReaderOrientation.FREE,
                    onChangeOrientation = {},
                )
            }
        }
    }
}
