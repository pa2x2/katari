package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.theme.KatariPreviewTheme
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The guide's last page: setup is done, with the getting started guide for someone new. */
internal class FinishStep : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.RocketLaunch,
        heading = stringResource(MR.strings.onboarding_finish_heading),
        subtitle = stringResource(MR.strings.onboarding_finish_subtitle, stringResource(MR.strings.app_name)),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current
        RowGroup {
            row {
                ControlRow {
                    Button(
                        label = stringResource(MR.strings.getting_started_guide),
                        onClick = { handler.openUri(GETTING_STARTED_URL) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

const val GETTING_STARTED_URL = "${Constants.URL_DOCS}first-steps/"

@PreviewLightDark
@Composable
private fun FinishStepPreview() {
    KatariPreviewTheme {
        FinishStep().Content()
    }
}
