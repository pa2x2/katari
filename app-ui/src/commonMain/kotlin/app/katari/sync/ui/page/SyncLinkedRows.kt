package app.katari.sync.ui.page

import androidx.compose.runtime.Composable
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncHeadline
import app.katari.sync.ui.status.icon
import app.katari.sync.ui.status.stopsSync
import app.katari.sync.ui.status.syncHeadlineText
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Logout
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page of a linked device (D050): the status card, what waits for the person, the account's devices with Link a
 * device, the options, and Disconnect.
 */
@Composable
internal fun linkedPreferences(
    link: SyncStatus.Link.Linked,
    status: SyncStatus,
    headline: SyncHeadline?,
    devices: SyncViewModel.DevicesState,
    preferences: SyncPreferences,
    actions: SyncPageActions,
): List<Preference> = buildList {
    add(statusCard(link, status, headline, actions))
    // In the status card, under the status they explain
    addAll(attentionRows(headline, link.server.toString(), actions))
    add(devicesGroup(devices, actions))
    add(syncOptionsGroup(preferences))
    add(
        Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.sync_disconnect)) {
            TextPreferenceWidget(
                title = stringResource(MR.strings.sync_disconnect),
                icon = MaterialSymbols.Rounded.Logout,
                onPreferenceClick = actions.disconnect,
            )
        },
    )
}

@Composable
private fun statusCard(
    link: SyncStatus.Link.Linked,
    status: SyncStatus,
    headline: SyncHeadline?,
    actions: SyncPageActions,
): Preference.PreferenceItem<*, *> {
    val account = link.accountName?.let { stringResource(MR.strings.sync_account_on_server, it, link.server.host) }
        ?: link.server.host
    return Preference.PreferenceItem.CustomPreference(title = "status") {
        TextPreferenceWidget(
            title = headline?.let { syncHeadlineText(it) }.orEmpty(),
            // The headline says when it last synced
            subtitle = account,
            icon = headline?.icon(),
            iconAlert = headline?.stopsSync() == true,
            widget = {
                Button(
                    label = stringResource(MR.strings.sync_action_sync_now),
                    onClick = actions.syncNow,
                    style = ButtonStyle.Tonal,
                    size = ButtonSize.Small,
                    enabled = link.stopped == null && status.activity !is SyncStatus.Activity.Syncing,
                )
            },
        )
    }
}

/** Rows for what waits for the person, each opening where it is settled. */
@Composable
private fun attentionRows(
    headline: SyncHeadline?,
    server: String,
    actions: SyncPageActions,
): List<Preference.PreferenceItem<*, *>> {
    val row: @Composable (String, () -> Unit) -> Preference.PreferenceItem<*, *> = { title, onClick ->
        Preference.PreferenceItem.CustomPreference(title = title) {
            TextPreferenceWidget(
                title = title,
                icon = MaterialSymbols.Rounded.SyncProblem,
                onPreferenceClick = onClick,
            )
        }
    }
    return when (headline) {
        SyncHeadline.ChoicesWaiting ->
            listOf(row(stringResource(MR.strings.sync_action_continue_choosing), actions.openChoices))
        is SyncHeadline.Questions ->
            listOf(row(stringResource(MR.strings.sync_action_answer_questions), actions.openQuestions))
        SyncHeadline.SignInAgain ->
            listOf(row(stringResource(MR.strings.sync_action_sign_in_again)) { actions.signIn(server) })
        else -> emptyList()
    }
}

/** The device's own sync options, which every device sets for itself (D022, D050). */
@Composable
internal fun syncOptionsGroup(preferences: SyncPreferences) = Preference.PreferenceGroup(
    title = stringResource(MR.strings.sync_options),
    preferenceItems = listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = preferences.useMobileData,
            title = stringResource(MR.strings.sync_use_mobile_data),
            subtitle = stringResource(MR.strings.sync_use_mobile_data_summary),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = preferences.syncInBackground,
            title = stringResource(MR.strings.sync_in_background),
            subtitle = stringResource(MR.strings.sync_in_background_summary),
        ),
    ),
)
