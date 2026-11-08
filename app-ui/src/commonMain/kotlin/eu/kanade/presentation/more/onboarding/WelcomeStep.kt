package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.RocketLaunch
import app.katari.presentation.kit.feedback.GuidePageHeading
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.RowGroup
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How someone starts with Katari, chosen on the guide's first page. It decides the page that follows. */
internal enum class OnboardingStart { New, Backup, Mihon }

/**
 * The guide's first page: whether the person starts new, from a backup, or from Mihon where it is installed. Linking
 * to a sync account has its own page later, which everyone passes.
 */
internal class WelcomeStep(
    private val start: OnboardingStart,
    private val onStartChange: (OnboardingStart) -> Unit,
    private val offersMihon: Boolean,
) : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.RocketLaunch,
        heading = stringResource(MR.strings.onboarding_welcome_heading, stringResource(MR.strings.app_name)),
        subtitle = stringResource(MR.strings.onboarding_welcome_subtitle),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val appName = stringResource(MR.strings.app_name)
        RowGroup {
            row {
                ChoiceRow(
                    title = stringResource(MR.strings.onboarding_start_new, appName),
                    subtitle = stringResource(MR.strings.onboarding_start_new_summary),
                    selected = start == OnboardingStart.New,
                    onClick = { onStartChange(OnboardingStart.New) },
                )
            }
            if (offersMihon) {
                row {
                    ChoiceRow(
                        title = stringResource(MR.strings.onboarding_mihon_migration_title),
                        subtitle = stringResource(MR.strings.onboarding_start_mihon_summary),
                        selected = start == OnboardingStart.Mihon,
                        onClick = { onStartChange(OnboardingStart.Mihon) },
                    )
                }
            }
            row {
                ChoiceRow(
                    title = stringResource(MR.strings.onboarding_start_backup),
                    subtitle = stringResource(MR.strings.onboarding_start_backup_summary, appName),
                    selected = start == OnboardingStart.Backup,
                    onClick = { onStartChange(OnboardingStart.Backup) },
                )
            }
        }
    }
}
