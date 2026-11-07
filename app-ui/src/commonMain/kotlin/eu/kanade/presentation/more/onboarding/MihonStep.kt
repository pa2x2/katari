package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupNote
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page after choosing to move from Mihon: make a backup in Mihon, then choose it here. The restore runs in the
 * background while the guide goes on, and the storage page then suggests Mihon's folder.
 */
internal class MihonStep(
    private val onOpenMihon: () -> Unit,
    private val onChooseMihonBackup: () -> Unit,
) : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.SwapHoriz,
        heading = stringResource(MR.strings.onboarding_mihon_migration_title),
        subtitle = stringResource(MR.strings.onboarding_mihon_migration_description),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column {
            RowGroup {
                row {
                    ControlRow {
                        Button(
                            label = stringResource(MR.strings.onboarding_mihon_migration_open),
                            onClick = onOpenMihon,
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Outlined,
                        )
                        Button(
                            label = stringResource(MR.strings.onboarding_mihon_migration_choose_backup),
                            onClick = onChooseMihonBackup,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            RowGroupNote(stringResource(MR.strings.onboarding_mihon_migration_note))
        }
    }
}
