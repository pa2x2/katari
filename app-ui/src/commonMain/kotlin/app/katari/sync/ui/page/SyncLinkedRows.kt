package app.katari.sync.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.icons.materialsymbols.rounded.Dns
import app.katari.icons.materialsymbols.rounded.Extension
import app.katari.icons.materialsymbols.rounded.Logout
import app.katari.icons.materialsymbols.rounded.Person
import app.katari.icons.materialsymbols.rounded.SyncDisabled
import app.katari.icons.materialsymbols.rounded.SyncProblem
import app.katari.missingextensions.model.MissingExtension
import app.katari.missingextensions.ui.noticeText
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.list.RowAction
import app.katari.presentation.kit.list.RowActions
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.ProfileSync
import app.katari.sync.client.part.ProfileSyncState
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.server.shownAddress
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.client.trigger.LiveUpdatesState
import app.katari.sync.ui.choice.whatSyncsSummary
import app.katari.sync.ui.guide.ServerFix
import app.katari.sync.ui.guide.SyncGuide
import app.katari.sync.ui.guide.rememberOpenSyncGuide
import app.katari.sync.ui.guide.serverFixLabel
import app.katari.sync.ui.status.SyncHeadline
import app.katari.sync.ui.status.icon
import app.katari.sync.ui.status.stopsSync
import app.katari.sync.ui.status.syncHeadlineText
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

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
    choices: ChoiceSet,
    profilesNotSynced: List<ProfileSync>,
    missingExtensions: List<MissingExtension>,
    actions: SyncPageActions,
): List<Preference> = buildList {
    add(statusCard(link, status, headline, actions))
    // In the status card, under the status they explain
    addAll(attentionRows(headline, link, actions))
    if (status.live == LiveUpdatesState.NOT_WORKING) add(liveUpdatesNotWorkingRow())
    profilesNotSynced.filter { it.state == ProfileSyncState.ASKING }.forEach { add(arrivingProfileRow(it, actions)) }
    if (missingExtensions.isNotEmpty()) add(missingExtensionsRow(missingExtensions, actions))
    if (status.heldAside > 0) {
        val count = status.heldAside.toInt()
        add(Preference.PreferenceItem.InfoPreference(pluralStringResource(MR.plurals.sync_held_aside, count, count)))
    }
    // The server no longer knows this device's sign-in, so it has no devices to list or link until it signs in again
    if (link.stopped != SyncStop.SIGN_IN_AGAIN && link.stopped != SyncStop.SERVER_RESET) {
        val reachable = status.activity !is SyncStatus.Activity.Unreachable &&
            status.activity != SyncStatus.Activity.Offline
        add(devicesGroup(devices, reachable, actions))
    }
    val linkedOptions = listOf(
        whatSyncsRow(choices, profilesNotSynced.map { it.name }, actions),
        serverAddressRow(link, actions),
    )
    add(syncOptionsGroup(preferences, linkedOptions))
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
            // The headline says when it last synced; a resync that reads like a first download says why it runs
            subtitle = if (headline is SyncHeadline.CatchingUp) {
                stringResource(MR.strings.sync_catching_up_text)
            } else {
                account
            },
            subtitleState = if (status.live == LiveUpdatesState.OPEN) {
                TonedSpan(stringResource(MR.strings.sync_live), ContentTone.Success)
            } else {
                null
            },
            icon = headline?.icon(),
            iconAlert = headline?.stopsSync() == true,
            widget = {
                Button(
                    label = stringResource(MR.strings.sync_action_sync_now),
                    onClick = actions.syncNow,
                    style = ButtonStyle.Tonal,
                    size = ButtonSize.Small,
                    enabled = status.canSyncNow,
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

/**
 * Changes from other devices come only with runs because the live channel keeps failing while the server answers,
 * opening the guide's section on what a proxy in front of the server has to pass on.
 */
@Composable
private fun liveUpdatesNotWorkingRow(): Preference.PreferenceItem<*, *> {
    val title = stringResource(MR.strings.sync_live_not_working)
    val openGuide = rememberOpenSyncGuide()
    return Preference.PreferenceItem.CustomPreference(title = title) {
        TextPreferenceWidget(
            title = title,
            subtitle = stringResource(MR.strings.sync_live_not_working_text),
            icon = MaterialSymbols.Rounded.SyncDisabled,
            onPreferenceClick = { openGuide(PROXY_GUIDE) },
        )
    }
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
    val signInAgain = { actions.signIn(link.server.shownAddress, link.accountName) }
    return when (headline) {
        SyncHeadline.ChoicesWaiting ->
            listOf(row(stringResource(MR.strings.sync_action_continue_choosing), actions.openChoices))
        is SyncHeadline.Questions ->
            listOf(row(stringResource(MR.strings.sync_action_answer_questions), actions.openQuestions))
        SyncHeadline.SignInAgain -> listOf(row(stringResource(MR.strings.sync_action_sign_in_again), signInAgain))
        // Signing in tells the person how to make the account again, and says so again if they haven't yet
        SyncHeadline.ServerReset -> {
            val title = stringResource(MR.strings.sync_action_set_up_again)
            listOf(
                Preference.PreferenceItem.CustomPreference(title = title) {
                    TextPreferenceWidget(
                        title = title,
                        subtitle = stringResource(MR.strings.sync_server_reset_text),
                        icon = MaterialSymbols.Rounded.Dns,
                        onPreferenceClick = signInAgain,
                    )
                },
            )
        }
        is SyncHeadline.Failed -> listOf(
            Preference.PreferenceItem.InfoPreference(stringResource(platformPresentation.syncFailedHelp)),
        )
        is SyncHeadline.ServerTooOld -> {
            val openGuide = rememberOpenSyncGuide()
            listOf(row(serverFixLabel(ServerFix.UPDATE)) { openGuide(ServerFix.UPDATE.page) })
        }
        else -> emptyList()
    }
}

/** A profile made on another device, asking whether it comes to this one. */
@Composable
private fun arrivingProfileRow(profile: ProfileSync, actions: SyncPageActions): Preference.PreferenceItem<*, *> {
    val title = stringResource(MR.strings.sync_profile_arriving, profile.name)
    return Preference.PreferenceItem.CustomPreference(title = title) {
        Column {
            TextPreferenceWidget(
                title = title,
                icon = MaterialSymbols.Rounded.Person,
            )
            RowActions(
                listOf(
                    RowAction(
                        label = stringResource(MR.strings.sync_profile_not_here),
                        onClick = { actions.answerArrivingProfile(profile, false) },
                    ),
                    RowAction(
                        label = stringResource(MR.strings.sync_profile_add_here),
                        onClick = { actions.answerArrivingProfile(profile, true) },
                    ),
                ),
            )
        }
    }
}

/** What this device syncs, summed up, opening the page that chooses it. */
@Composable
private fun whatSyncsRow(
    choices: ChoiceSet,
    profilesNotSynced: List<String>,
    actions: SyncPageActions,
): Preference.PreferenceItem<out Any, out Any> {
    val title = stringResource(MR.strings.sync_choices_title)
    return Preference.PreferenceItem.CustomPreference(title = title) {
        TextPreferenceWidget(
            title = title,
            subtitle = whatSyncsSummary(choices, profilesNotSynced),
            icon = MaterialSymbols.Rounded.CloudSync,
            onPreferenceClick = actions.openWhatSyncs,
        )
    }
}

/** Where this device's server is, opening the dialog that follows it to a new address. */
@Composable
private fun serverAddressRow(
    link: SyncStatus.Link.Linked,
    actions: SyncPageActions,
): Preference.PreferenceItem<out Any, out Any> {
    val title = stringResource(MR.strings.sync_server_address)
    return Preference.PreferenceItem.CustomPreference(title = title) {
        TextPreferenceWidget(
            title = title,
            subtitle = link.server.shownAddress,
            icon = MaterialSymbols.Rounded.Dns,
            onPreferenceClick = actions.changeServer,
        )
    }
}

/**
 * The device's own sync options, which every device sets for itself (D022, D050), led by [leading] rows once linked:
 * what syncs and the server's address. Where the platform can't tell Wi-Fi, the mobile data switches hold files and
 * changes back on networks the system marks metered, and say so.
 */
@Composable
internal fun syncOptionsGroup(
    preferences: SyncPreferences,
    leading: List<Preference.PreferenceItem<out Any, out Any>> = emptyList(),
): Preference.PreferenceGroup {
    val inBackground by preferences.syncInBackground.collectAsState()
    val intervals = SyncPreferences.BACKGROUND_INTERVALS.associateWith { stringResource(intervalName(it)) }
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.sync_options),
        preferenceItems = leading + listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.paused,
                title = stringResource(MR.strings.sync_pause),
                subtitle = stringResource(MR.strings.sync_pause_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.useMobileData,
                title = stringResource(platformPresentation.syncFilesOnMeteredLabel),
                subtitle = platformPresentation.syncFilesOnMeteredSummary?.let { stringResource(it) },
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.changesOnMobileData,
                title = stringResource(platformPresentation.syncChangesOnMeteredLabel),
                subtitle = stringResource(platformPresentation.syncChangesOnMeteredSummary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.syncInBackground,
                title = stringResource(MR.strings.sync_in_background),
                subtitle = stringResource(MR.strings.sync_in_background_summary),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = preferences.backgroundInterval,
                entries = intervals,
                title = stringResource(MR.strings.sync_background_interval),
                visible = inBackground,
            ),
        ),
    )
}

private fun intervalName(minutes: Int): StringResource = when (minutes) {
    15 -> MR.strings.update_15min
    30 -> MR.strings.update_30min
    60 -> MR.strings.update_1hour
    180 -> MR.strings.update_3hour
    360 -> MR.strings.update_6hour
    720 -> MR.strings.update_12hour
    1440 -> MR.strings.update_24hour
    else -> error("No name for syncing every $minutes minutes")
}

/** The server guide's section on running it behind a proxy, which says what a proxy must pass on for live updates. */
private const val PROXY_GUIDE = "${SyncGuide.SERVER}#https"
