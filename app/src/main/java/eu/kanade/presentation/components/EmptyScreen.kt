package eu.kanade.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.Help
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.EmptyScreenAction
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.theme.KatariPreviewTheme
import tachiyomi.i18n.*

@PreviewLightDark
@Composable
private fun NoActionPreview() {
    KatariPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WithActionPreview() {
    KatariPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
                actions = listOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = {},
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.getting_started_guide,
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onClick = {},
                    ),
                ),
            )
        }
    }
}
