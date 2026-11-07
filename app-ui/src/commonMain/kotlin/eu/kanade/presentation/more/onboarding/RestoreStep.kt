package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.SettingsBackupRestore
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page after choosing to start from a backup: picking the file opens the restore's options, and the restore runs in
 * the background while the guide goes on.
 */
internal class RestoreStep : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.SettingsBackupRestore,
        heading = stringResource(MR.strings.pref_restore_backup),
        subtitle = stringResource(MR.strings.onboarding_restore_subtitle, stringResource(MR.strings.app_name)),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val restoreBackup = rememberRestoreBackupLauncher()
        RowGroup {
            row {
                ControlRow {
                    Button(
                        label = stringResource(MR.strings.onboarding_restore_choose),
                        onClick = restoreBackup,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
