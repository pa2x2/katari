package app.katari.sync.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CheckCircle
import app.katari.icons.materialsymbols.rounded.Dns
import app.katari.icons.materialsymbols.rounded.Keyboard
import app.katari.icons.materialsymbols.rounded.PhonelinkErase
import app.katari.icons.materialsymbols.rounded.QrCodeScanner
import app.katari.icons.materialsymbols.rounded.Smartphone
import app.katari.presentation.kit.button.PageAction
import app.katari.presentation.kit.button.PageActions
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.server.shownAddress
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.guide.SyncGuide
import app.katari.sync.ui.guide.SyncGuideAction
import app.katari.sync.ui.link.linkCodeScanning
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page before setup (D027): what syncs and what stays, each opening What syncs to choose before the first sync,
 * then the ways in. Where the device has nowhere to keep its
 * sign-in, such as a desktop without a keyring, it says so first (D061), and signing in still says so if tried.
 */
@Composable
internal fun setUpPreferences(canKeepSignIn: Boolean, actions: SyncPageActions): List<Preference> = buildList {
    add(
        Preference.PreferenceItem.CustomPreference(title = "intro", framed = false) {
            RowGroupNote(stringResource(MR.strings.sync_intro))
        },
    )
    add(
        Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.sync_what_syncs_title)) {
            TextPreferenceWidget(
                title = stringResource(MR.strings.sync_what_syncs_title),
                subtitle = stringResource(MR.strings.sync_what_syncs),
                icon = MaterialSymbols.Rounded.CheckCircle,
                onPreferenceClick = actions.openWhatSyncs,
            )
        },
    )
    add(
        Preference.PreferenceItem.CustomPreference(title = stringResource(MR.strings.sync_what_stays_title)) {
            TextPreferenceWidget(
                title = stringResource(MR.strings.sync_what_stays_title),
                subtitle = stringResource(MR.strings.sync_what_stays),
                icon = MaterialSymbols.Rounded.Smartphone,
                onPreferenceClick = actions.openWhatSyncs,
            )
        },
    )
    if (!canKeepSignIn) {
        add(
            Preference.PreferenceItem.CustomPreference(title = "keyring", framed = false) {
                Column {
                    RowGroupGap()
                    NoticeCard(stringResource(MR.strings.sync_no_keyring), NoticeTone.Warning)
                }
            },
        )
    }
    add(Preference.PreferenceItem.CustomPreference(title = "ways-in", framed = false) { WaysIn(actions, null) })
    add(
        Preference.PreferenceItem.CustomPreference(title = "no-server", framed = false) {
            SyncGuideAction(stringResource(MR.strings.sync_guide_no_server), SyncGuide.SERVER)
        },
    )
}

/** A device removed from its account, or whose account was disabled (D030): standalone again, and why. */
@Composable
internal fun endedPreferences(link: SyncStatus.Link.Ended, actions: SyncPageActions): List<Preference> = listOf(
    Preference.PreferenceItem.CustomPreference(title = "ended") {
        TextPreferenceWidget(
            title = stringResource(
                if (link.reason == SyncStop.ACCOUNT_DISABLED) {
                    MR.strings.sync_account_disabled_title
                } else {
                    MR.strings.sync_removed_title
                },
            ),
            subtitle = stringResource(MR.strings.sync_ended_text, link.server?.host.orEmpty()),
            icon = MaterialSymbols.Rounded.PhonelinkErase,
            iconAlert = true,
        )
    },
    Preference.PreferenceItem.CustomPreference(title = "ways-in", framed = false) {
        PageActions(
            main = PageAction(
                label = stringResource(MR.strings.sync_action_sign_in_again),
                onClick = { actions.signIn(link.server?.shownAddress, link.accountName) },
                icon = MaterialSymbols.Rounded.Dns,
            ),
            others = listOf(PageAction(stringResource(MR.strings.action_close), actions.dismissEnded)),
        )
    },
)

@Composable
private fun WaysIn(actions: SyncPageActions, server: String?) {
    PageActions(
        main = PageAction(
            label = stringResource(MR.strings.sync_action_sign_in_with_address),
            onClick = { actions.signIn(server, null) },
            icon = MaterialSymbols.Rounded.Dns,
        ),
        others = listOf(
            if (linkCodeScanning) {
                PageAction(
                    label = stringResource(MR.strings.sync_action_scan_code),
                    onClick = actions.enterCode,
                    icon = MaterialSymbols.Rounded.QrCodeScanner,
                )
            } else {
                PageAction(
                    label = stringResource(MR.strings.sync_action_enter_code),
                    onClick = actions.enterCode,
                    icon = MaterialSymbols.Rounded.Keyboard,
                )
            },
        ),
    )
}
