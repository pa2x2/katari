package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.link.linkCodeEntryScreen
import app.katari.sync.ui.link.linkCodeScanning
import app.katari.sync.ui.signin.SyncSignInScreen
import app.katari.sync.ui.status.SyncStatusState
import app.katari.sync.ui.status.icon
import app.katari.sync.ui.status.rememberSyncHeadline
import app.katari.sync.ui.status.rememberSyncStatus
import app.katari.sync.ui.status.syncHeadlineText
import app.katari.sync.ui.status.tone
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CheckCircle
import mihon.icons.materialsymbols.rounded.Devices
import mihon.icons.materialsymbols.rounded.Dns
import mihon.icons.materialsymbols.rounded.Keyboard
import mihon.icons.materialsymbols.rounded.QrCodeScanner
import mihon.icons.materialsymbols.rounded.Smartphone
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The guide's sync page, after permissions: what syncs and what stays, then the two ways in, which open the sign-in or
 * code screens and come back here. Skip goes on without linking; once linked, the page shows the account and the
 * first download's progress, and the guide goes on while it runs.
 */
internal class SyncStep(statusState: SyncStatusState) : OnboardingStep {

    private var linked by mutableStateOf(statusState.status.value?.link as? SyncStatus.Link.Linked)

    @Composable
    override fun heading(): GuidePageHeading {
        val link = linked
        return if (link == null) {
            GuidePageHeading(
                icon = MaterialSymbols.Rounded.Devices,
                heading = stringResource(MR.strings.onboarding_sync_heading, stringResource(MR.strings.app_name)),
                subtitle = stringResource(MR.strings.onboarding_sync_subtitle),
            )
        } else {
            GuidePageHeading(
                icon = MaterialSymbols.Rounded.CheckCircle,
                heading = stringResource(MR.strings.onboarding_sync_linked_heading),
                subtitle = link.accountName
                    ?.let { stringResource(MR.strings.onboarding_sync_linked_account, it, link.server.host) }
                    ?: link.server.host,
            )
        }
    }

    override val isComplete: Boolean
        get() = linked != null

    override val canSkip: Boolean
        get() = linked == null

    @Composable
    override fun Content() {
        val link = rememberSyncStatus()?.link
        LaunchedEffect(link) { linked = link as? SyncStatus.Link.Linked }

        if (link is SyncStatus.Link.Linked) LinkedStatus() else WaysIn()
    }

    @Composable
    private fun LinkedStatus() {
        val headline = rememberSyncHeadline() ?: return
        RowGroup {
            row {
                SettingsRow(
                    title = stringResource(MR.strings.sync_title),
                    subtitle = syncHeadlineText(headline),
                    subtitleTone = headline.tone(),
                    icon = headline.icon(),
                )
            }
        }
    }

    @Composable
    private fun WaysIn() {
        val navigator = LocalNavigator.currentOrThrow
        Column {
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.sync_what_syncs_title),
                        subtitle = stringResource(MR.strings.sync_what_syncs),
                        icon = MaterialSymbols.Rounded.CheckCircle,
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.sync_what_stays_title),
                        subtitle = stringResource(MR.strings.sync_what_stays),
                        icon = MaterialSymbols.Rounded.Smartphone,
                    )
                }
            }
            RowGroupGap()
            RowGroup {
                row {
                    SettingsRow(
                        title = if (linkCodeScanning) {
                            stringResource(MR.strings.sync_action_scan_code)
                        } else {
                            stringResource(MR.strings.sync_action_enter_code)
                        },
                        subtitle = stringResource(MR.strings.onboarding_sync_code_summary),
                        icon = if (linkCodeScanning) {
                            MaterialSymbols.Rounded.QrCodeScanner
                        } else {
                            MaterialSymbols.Rounded.Keyboard
                        },
                        onClick = { navigator.push(linkCodeEntryScreen()) },
                    )
                }
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.sync_action_sign_in_with_address),
                        subtitle = stringResource(MR.strings.onboarding_sync_sign_in_summary),
                        icon = MaterialSymbols.Rounded.Dns,
                        onClick = { navigator.push(SyncSignInScreen()) },
                    )
                }
            }
        }
    }
}
