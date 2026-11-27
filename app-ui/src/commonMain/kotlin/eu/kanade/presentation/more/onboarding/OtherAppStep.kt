package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.backupconversion.BringOverScreen
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SwapHoriz
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.feedback.GuidePageHeading
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupNote
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page after choosing to come from another app (D019, D020): make a backup there, then bring it over with the tool,
 * which opens over the guide. Its restore runs in the background while the guide goes on.
 */
internal class OtherAppStep : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.SwapHoriz,
        heading = stringResource(MR.strings.onboarding_other_app_title),
        subtitle = stringResource(MR.strings.onboarding_other_app_subtitle, stringResource(MR.strings.app_name)),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Column {
            RowGroup {
                row {
                    ControlRow {
                        Button(
                            label = stringResource(MR.strings.backup_conversion_title),
                            onClick = { navigator.push(BringOverScreen()) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            platformPresentation.otherAppExtensionsNote?.let { RowGroupNote(stringResource(it)) }
        }
    }
}
