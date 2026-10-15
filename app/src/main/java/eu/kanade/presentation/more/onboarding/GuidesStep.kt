package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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

        Column(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(stringResource(MR.strings.onboarding_guides_new_user, stringResource(MR.strings.app_name)))
            Button(
                label = stringResource(MR.strings.getting_started_guide),
                onClick = { handler.openUri(GETTING_STARTED_URL) },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = KatariTheme.spacing.s))

            if (showMihonMigration) {
                Text(
                    text = stringResource(MR.strings.onboarding_mihon_migration_title),
                    role = TextRole.SectionTitle,
                )
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
                Text(
                    text = stringResource(MR.strings.onboarding_mihon_migration_note),
                    role = TextRole.Supporting,
                )
            } else {
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
