package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.core.platform.currentPlatformContext
import app.katari.ui.di.LocalAppUiGraph
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsSecurityScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_security

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val securityPreferences = graph.securityPreferences
        val privacyPreferences = graph.privacyPreferences
        return buildList(2) {
            add(getSecurityGroup(securityPreferences))
            if (!graph.appBuild.telemetryIncluded) return@buildList
            add(getFirebaseGroup(privacyPreferences))
        }
    }

    @Composable
    private fun getSecurityGroup(
        securityPreferences: SecurityPreferences,
    ): Preference.PreferenceGroup {
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val authentication = graph.settingChangeAuthentication
        val authSupported = remember { graph.profileUnlock.isAvailable(context) }
        val wording = graph.profileUnlock.lockSettingsWording
        val secureScreenOffered = graph.secureScreen.isOffered
        val notificationsOffered = graph.appNotifications.isOffered
        val useAuthPref = securityPreferences.useAuthenticator
        val useAuth by useAuthPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_security),
            visible = authSupported || wording != null || notificationsOffered || secureScreenOffered,
            preferenceItems = listOfNotNull(
                wording?.takeUnless { authSupported }?.let {
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.profile_lock_unavailable),
                        subtitle = stringResource(it.unavailable),
                    )
                },
                Preference.PreferenceItem.SwitchPreference(
                    preference = useAuthPref,
                    title = stringResource(MR.strings.lock_with_biometrics),
                    subtitle = wording?.let { stringResource(it.requireUnlock) },
                    visible = authSupported,
                    onValueChanged = {
                        authentication.authenticate(
                            context = context,
                            title = context.stringResource(MR.strings.lock_with_biometrics),
                        )
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = securityPreferences.lockAppAfter,
                    entries = LockAfterValues
                        .associateWith {
                            when (it) {
                                SecurityPreferences.LOCK_NEVER -> stringResource(MR.strings.lock_never)
                                SecurityPreferences.LOCK_ALWAYS -> stringResource(MR.strings.lock_always)
                                else -> pluralStringResource(MR.plurals.lock_after_mins, count = it, it)
                            }
                        },
                    title = stringResource(MR.strings.lock_when_idle),
                    subtitleProvider = { value, entries ->
                        val chosen = entries[value].orEmpty()
                        wording?.let { stringResource(it.lockWhenIdle, chosen) } ?: chosen
                    },
                    visible = authSupported && useAuth,
                    onValueChanged = {
                        authentication.authenticate(
                            context = context,
                            title = context.stringResource(MR.strings.lock_when_idle),
                        )
                    },
                ),

                Preference.PreferenceItem.SwitchPreference(
                    preference = securityPreferences.hideNotificationContent,
                    title = stringResource(MR.strings.hide_notification_content),
                    visible = notificationsOffered,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = securityPreferences.secureScreen,
                    entries = SecurityPreferences.SecureScreenMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.secure_screen),
                    visible = secureScreenOffered,
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.secure_screen_summary))
                    .takeIf { secureScreenOffered },
            ),
        )
    }

    @Composable
    private fun getFirebaseGroup(
        privacyPreferences: PrivacyPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_firebase),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = privacyPreferences.crashlytics,
                    title = stringResource(MR.strings.onboarding_permission_crashlytics),
                    subtitle = stringResource(MR.strings.onboarding_permission_crashlytics_description),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = privacyPreferences.analytics,
                    title = stringResource(MR.strings.onboarding_permission_analytics),
                    subtitle = stringResource(MR.strings.onboarding_permission_analytics_description),
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.firebase_summary)),
            ),
        )
    }
}

private val LockAfterValues = listOf(
    SecurityPreferences.LOCK_ALWAYS,
    1,
    2,
    5,
    10,
    SecurityPreferences.LOCK_NEVER,
)

private val SecurityPreferences.SecureScreenMode.titleRes: StringResource
    get() = when (this) {
        SecurityPreferences.SecureScreenMode.ALWAYS -> MR.strings.lock_always
        SecurityPreferences.SecureScreenMode.INCOGNITO -> MR.strings.pref_incognito_mode
        SecurityPreferences.SecureScreenMode.NEVER -> MR.strings.lock_never
    }
