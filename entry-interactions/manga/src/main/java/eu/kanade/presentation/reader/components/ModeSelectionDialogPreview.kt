package eu.kanade.presentation.reader.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.entry.viewer.chrome.ui.ModeSelectionDialog
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.KatariPreviewTheme

@PreviewLightDark
@Composable
private fun Preview() {
    KatariPreviewTheme {
        Surface {
            Column {
                ModeSelectionDialog(
                    onApply = {},
                    onUseDefault = {},
                ) {
                    Text("Dummy content")
                }

                ModeSelectionDialog(
                    onApply = {},
                ) {
                    Text("Dummy content without default")
                }
            }
        }
    }
}
