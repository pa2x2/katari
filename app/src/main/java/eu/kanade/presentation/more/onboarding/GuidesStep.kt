package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.theme.KatariPreviewTheme
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
    private val showMihonMigration: Boolean,
    private val onOpenMihon: () -> Unit,
    private val onMigrateFromMihon: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current

        Column {
            RowGroup(
                rows = listOf {
                    ControlRow {
                        Text(stringResource(MR.strings.onboarding_guides_new_user, stringResource(MR.strings.app_name)))
                        Button(
                            label = stringResource(MR.strings.getting_started_guide),
                            onClick = { handler.openUri(GETTING_STARTED_URL) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
            RowGroupGap()
            if (showMihonMigration) {
                RowGroupHeader(stringResource(MR.strings.onboarding_mihon_migration_title))
                RowGroup(
                    rows = listOf {
                        ControlRow {
                            Text(stringResource(MR.strings.onboarding_mihon_migration_description))
                            Button(
                                label = stringResource(MR.strings.onboarding_mihon_migration_open),
                                onClick = onOpenMihon,
                                modifier = Modifier.fillMaxWidth(),
                                style = ButtonStyle.Outlined,
                            )
                            Button(
                                label = stringResource(MR.strings.onboarding_mihon_migration_choose_backup),
                                onClick = onMigrateFromMihon,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                )
                RowGroupNote(stringResource(MR.strings.onboarding_mihon_migration_note))
            } else {
                RowGroup(
                    rows = listOf {
                        ControlRow {
                            Text(
                                stringResource(
                                    MR.strings.onboarding_guides_returning_user,
                                    stringResource(MR.strings.app_name),
                                ),
                            )
                            Button(
                                label = stringResource(MR.strings.pref_restore_backup),
                                onClick = onRestoreBackup,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                )
            }
        }
    }
}

const val GETTING_STARTED_URL = "${Constants.URL_GUIDE}first-steps/"

@PreviewLightDark
@Composable
private fun GuidesStepPreview() {
    KatariPreviewTheme {
        GuidesStep(
            onRestoreBackup = {},
            showMihonMigration = true,
            onOpenMihon = {},
            onMigrateFromMihon = {},
        ).Content()
    }
}
