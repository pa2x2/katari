package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.core.platform.currentPlatformContext
import app.katari.notification.LibraryUpdatesNotificationKind
import app.katari.notification.NotificationGroup
import app.katari.notification.NotificationKind
import app.katari.notification.NotificationPreferences
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.notifications.NotificationChoices
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

/**
 * Which notifications show, and whether they hide their content. Where the system keeps the choices (Android), a row
 * opens its settings; where the app keeps them (the desktop), one switch turns them all off, and while it is on, a
 * switch for each kind, grouped as Android groups its channels. Each profile's library updates are a kind of their own,
 * as on Android.
 */
class SettingsNotificationsScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_notifications

    override fun isOffered(graph: AppUiGraph): Boolean = graph.appNotifications.isOffered

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val hideContent = Preference.PreferenceItem.SwitchPreference(
            preference = graph.securityPreferences.hideNotificationContent,
            title = stringResource(MR.strings.hide_notification_content),
        )
        return when (val choices = graph.appNotifications.choices) {
            is NotificationChoices.InSystemSettings -> {
                val context = currentPlatformContext()
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_manage_notifications),
                        subtitle = stringResource(MR.strings.pref_manage_notifications_in_system_summary),
                        onClick = { choices.open(context) },
                    ),
                    hideContent,
                )
            }
            is NotificationChoices.InApp -> inAppPreferences(choices, hideContent)
        }
    }

    @Composable
    private fun inAppPreferences(
        choices: NotificationChoices.InApp,
        hideContent: Preference.PreferenceItem.SwitchPreference,
    ): List<Preference> {
        val graph = LocalAppUiGraph.current
        val preferences = graph.notificationPreferences
        val enabled by preferences.enabled.collectPreferenceAsState()
        val profiles by graph.profileManager.visibleProfiles.collectAsState()
        val enabledSwitch = Preference.PreferenceItem.SwitchPreference(
            preference = preferences.enabled,
            title = stringResource(MR.strings.pref_notifications_enabled),
        )
        if (!enabled) return listOf(enabledSwitch)
        val libraryUpdates = profiles.map { profile ->
            val title = if (profiles.size > 1) {
                stringResource(MR.strings.channel_library_updates_profile, profile.name)
            } else {
                stringResource(MR.strings.channel_library_updates)
            }
            preferences.switch(LibraryUpdatesNotificationKind(profile.id), title)
        }
        val grouped = NotificationGroup.entries.map { group ->
            val kinds = choices.kinds.filter { it.group == group }
                .map { preferences.switch(it, stringResource(it.title)) }
            val rows = if (group == NotificationGroup.LIBRARY) libraryUpdates + kinds else kinds
            stringResource(group.title) to rows
        }
        val other = stringResource(MR.strings.notification_kinds_other) to
            choices.kinds.filter { it.group == null }.map { preferences.switch(it, stringResource(it.title)) }
        return listOf(enabledSwitch, hideContent) + (grouped + other)
            .filter { (_, rows) -> rows.isNotEmpty() }
            .map { (title, rows) -> Preference.PreferenceGroup(title = title, preferenceItems = rows) }
    }

    private fun NotificationPreferences.switch(kind: NotificationKind, title: String) =
        Preference.PreferenceItem.SwitchPreference(preference = shows(kind), title = title)
}
