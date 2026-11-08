package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.SettingsBackupRestore
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.feedback.GuidePageHeading
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroup
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
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
