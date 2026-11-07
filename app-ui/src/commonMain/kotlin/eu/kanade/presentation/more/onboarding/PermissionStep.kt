package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.VerifiedUser
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.ToggleRow
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * The system permissions the platform has, then the build's telemetry switches. The guide skips the step where there
 * are neither.
 */
internal class PermissionStep(
    private val permissions: OnboardingPermissions,
    private val privacyPreferences: PrivacyPreferences,
    private val telemetryIncluded: Boolean,
) : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.VerifiedUser,
        heading = stringResource(MR.strings.onboarding_permissions_heading),
        subtitle = stringResource(MR.strings.onboarding_permissions_subtitle, stringResource(MR.strings.app_name)),
    )

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        Column {
            if (permissions.isOffered) {
                val requests = permissions.rememberRequests()
                RowGroup(
                    rows = requests.map { request ->
                        @Composable {
                            PermissionCheckbox(
                                title = stringResource(request.title),
                                subtitle = stringResource(request.description),
                                granted = request.granted,
                                onButtonClick = request.request,
                            )
                        }
                    },
                )
            }
            if (telemetryIncluded) {
                if (permissions.isOffered) RowGroupGap()
                RowGroup(
                    rows = listOf(
                        {
                            val crashlyticsPref = privacyPreferences.crashlytics
                            val crashlytics by crashlyticsPref.collectAsState()
                            PermissionSwitch(
                                title = stringResource(MR.strings.onboarding_permission_crashlytics),
                                subtitle = stringResource(MR.strings.onboarding_permission_crashlytics_description),
                                granted = crashlytics,
                                onToggleChange = crashlyticsPref::set,
                            )
                        },
                        {
                            val analyticsPref = privacyPreferences.analytics
                            val analytics by analyticsPref.collectAsState()
                            PermissionSwitch(
                                title = stringResource(MR.strings.onboarding_permission_analytics),
                                subtitle = stringResource(MR.strings.onboarding_permission_analytics_description),
                                granted = analytics,
                                onToggleChange = analyticsPref::set,
                            )
                        },
                    ),
                )
            }
        }
    }

    @Composable
    private fun PermissionCheckbox(
        title: String,
        subtitle: String,
        granted: Boolean,
        onButtonClick: () -> Unit,
    ) {
        SettingsRow(
            title = title,
            subtitle = subtitle,
            trailing = {
                if (granted) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Check,
                        contentDescription = null,
                        tone = ContentTone.Primary,
                    )
                } else {
                    Button(
                        label = stringResource(MR.strings.onboarding_permission_action_grant),
                        onClick = onButtonClick,
                        style = ButtonStyle.Outlined,
                    )
                }
            },
        )
    }

    @Composable
    private fun PermissionSwitch(
        title: String,
        subtitle: String,
        granted: Boolean,
        onToggleChange: (Boolean) -> Unit,
    ) {
        ToggleRow(title = title, checked = granted, onCheckedChange = onToggleChange, subtitle = subtitle)
    }
}
