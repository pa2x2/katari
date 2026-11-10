package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * A switch for each kind of notification, where the app rather than the system keeps them (the desktop), grouped as
 * Android groups its channels. Each profile's library updates are a kind of their own, as on Android.
 */
class SettingsNotificationsScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_notifications

    override fun isOffered(graph: AppUiGraph): Boolean =
        graph.appNotifications.isOffered && graph.appNotifications.choices is NotificationChoices.InApp

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val choices = graph.appNotifications.choices as? NotificationChoices.InApp ?: return emptyList()
        val preferences = graph.notificationPreferences
        val profiles by graph.profileManager.visibleProfiles.collectAsState()
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
        return (grouped + other)
            .filter { (_, rows) -> rows.isNotEmpty() }
            .map { (title, rows) -> Preference.PreferenceGroup(title = title, preferenceItems = rows) }
    }

    private fun NotificationPreferences.switch(kind: NotificationKind, title: String) =
        Preference.PreferenceItem.SwitchPreference(preference = shows(kind), title = title)
}
