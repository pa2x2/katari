package app.katari.sync.ui.page

import androidx.compose.runtime.Composable
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.server.shownAddress
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncHeadline
import app.katari.sync.ui.status.icon
import app.katari.sync.ui.status.stopsSync
import app.katari.sync.ui.status.syncHeadlineText
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import mihon.feature.missingextensions.model.MissingExtension
import mihon.feature.missingextensions.ui.noticeText
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Extension
import mihon.icons.materialsymbols.rounded.Logout
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page of a linked device (D050): the status card, what waits for the person, how many records are held aside
 * (D067), the account's devices with Link a device, the options, and Disconnect.
 */
@Composable
internal fun linkedPreferences(
    link: SyncStatus.Link.Linked,
    status: SyncStatus,
    headline: SyncHeadline?,
    devices: SyncViewModel.DevicesState,
    preferences: SyncPreferences,
    knowsWifi: Boolean,
    missingExtensions: List<MissingExtension>,
    actions: SyncPageActions,
): List<Preference> = buildList {
    add(statusCard(link, status, headline, actions))
    // In the status card, under the status they explain
    addAll(attentionRows(headline, link, actions))
    if (missingExtensions.isNotEmpty()) add(missingExtensionsRow(missingExtensions, actions))
    if (status.heldAside > 0) {
        val count = status.heldAside.toInt()
        add(Preference.PreferenceItem.InfoPreference(pluralStringResource(MR.plurals.sync_held_aside, count, count)))
    }
    // The server no longer knows this device's sign-in, so it has no devices to list or link until it signs in again
    if (link.stopped != SyncStop.SIGN_IN_AGAIN) {
        val reachable = status.activity !is SyncStatus.Activity.Unreachable &&
            status.activity != SyncStatus.Activity.Offline
        add(devicesGroup(devices, reachable, actions))
    }
    add(syncOptionsGroup(preferences, knowsWifi))
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
    // A headline that names the server leaves the account alone under it
    val accountName = link.accountName
    val account = when {
        accountName == null -> link.server.host
        headline?.namesServer() == true -> accountName
        else -> stringResource(MR.strings.sync_account_on_server, accountName, link.server.host)
    }
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
                    // A run does nothing while the first-sync choices wait, as while syncing is stopped
                    enabled = link.stopped == null &&
                        status.activity !is SyncStatus.Activity.Syncing &&
                        status.activity != SyncStatus.Activity.WaitingForChoices,
                )
            },
        )
    }
}

/** Whether the status card's headline names the server, as "Can't reach 127.0.0.1" does. */
private fun SyncHeadline.namesServer(): Boolean = when (this) {
    is SyncHeadline.Unreachable, is SyncHeadline.UnreachableLong, is SyncHeadline.ServerTooOld -> true
    is SyncHeadline.AccountDisabled -> server != null
    is SyncHeadline.Unsent -> unreachableServer != null
    else -> false
}

/** Library titles whose extensions this device lacks, opening the sheet that installs them. */
@Composable
private fun missingExtensionsRow(
    extensions: List<MissingExtension>,
    actions: SyncPageActions,
): Preference.PreferenceItem<*, *> {
    val title = stringResource(MR.strings.missing_extensions_entry)
    return Preference.PreferenceItem.CustomPreference(title = title) {
        TextPreferenceWidget(
            title = title,
            subtitle = noticeText(extensions),
            icon = MaterialSymbols.Rounded.Extension,
            onPreferenceClick = actions.openMissingExtensions,
        )
    }
}

/** Rows for what waits for the person, each opening where it is settled. */
@Composable
private fun attentionRows(
    headline: SyncHeadline?,
    link: SyncStatus.Link.Linked,
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
            listOf(
                row(stringResource(MR.strings.sync_action_sign_in_again)) {
                    actions.signIn(link.server.shownAddress, link.accountName)
                },
            )
        is SyncHeadline.Failed -> listOf(
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.sync_failed_help)),
        )
        else -> emptyList()
    }
}

/**
 * The device's own sync options, which every device sets for itself (D022, D050). Where the platform can't tell Wi-Fi
 * ([knowsWifi] false), the mobile data switch holds files back on networks the system marks metered, and says so.
 */
@Composable
internal fun syncOptionsGroup(preferences: SyncPreferences, knowsWifi: Boolean) = Preference.PreferenceGroup(
    title = stringResource(MR.strings.sync_options),
    preferenceItems = listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = preferences.useMobileData,
            title = stringResource(if (knowsWifi) MR.strings.sync_use_mobile_data else MR.strings.sync_use_metered),
            subtitle = stringResource(
                if (knowsWifi) MR.strings.sync_use_mobile_data_summary else MR.strings.sync_use_metered_summary,
            ),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = preferences.syncInBackground,
            title = stringResource(MR.strings.sync_in_background),
            subtitle = stringResource(MR.strings.sync_in_background_summary),
        ),
    ),
)
