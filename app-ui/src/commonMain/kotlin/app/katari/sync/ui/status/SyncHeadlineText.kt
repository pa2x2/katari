package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CloudDone
import mihon.icons.materialsymbols.rounded.CloudDownload
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.CloudSync
import mihon.icons.materialsymbols.rounded.PhonelinkErase
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.rounded.SyncProblem
import mihon.icons.materialsymbols.rounded.Wifi
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** How a Sync row words [headline]. */
@Composable
fun syncHeadlineText(headline: SyncHeadline): String = when (headline) {
    SyncHeadline.NotSetUp -> stringResource(MR.strings.sync_summary_standalone)
    is SyncHeadline.Removed -> stringResource(MR.strings.sync_status_removed)
    is SyncHeadline.AccountDisabled ->
        stringResource(MR.strings.sync_status_account_disabled, headline.server.orEmpty())
    SyncHeadline.SignInAgain -> stringResource(MR.strings.sync_status_sign_in_again)
    SyncHeadline.AppTooOld -> stringResource(MR.strings.sync_status_app_too_old)
    is SyncHeadline.ServerTooOld -> stringResource(MR.strings.sync_status_server_too_old, headline.server)
    SyncHeadline.ChoicesWaiting -> stringResource(MR.strings.sync_status_choices)
    is SyncHeadline.Questions ->
        pluralStringResource(MR.plurals.sync_questions_waiting, headline.count, headline.count)
    is SyncHeadline.UnreachableLong -> stringResource(
        MR.strings.sync_status_unreachable_long,
        headline.server,
        relativeTimeSpanString(headline.sinceMillis),
    )
    is SyncHeadline.HeldKinds -> stringResource(MR.strings.sync_status_held_kinds, heldKindsText(headline.kinds))
    is SyncHeadline.Downloading ->
        headline.progress
            ?.let { stringResource(MR.strings.sync_status_downloading, (it * 100).toInt()) }
            ?: stringResource(MR.strings.sync_status_downloading_unknown)
    SyncHeadline.FirstDownloadWaitsForWifi -> stringResource(MR.strings.sync_status_first_download_wifi)
    SyncHeadline.Syncing -> stringResource(MR.strings.sync_status_syncing)
    is SyncHeadline.Unsent -> stringResource(
        MR.strings.sync_status_with_note,
        pluralStringResource(MR.plurals.sync_unsent, headline.count.toInt(), headline.count.toInt()),
        headline.unreachableServer
            ?.let { stringResource(MR.strings.sync_unsent_unreachable, it) }
            ?: stringResource(MR.strings.sync_unsent_no_connection),
    )
    SyncHeadline.Offline -> stringResource(MR.strings.sync_status_offline)
    is SyncHeadline.Unreachable -> stringResource(MR.strings.sync_status_unreachable, headline.server)
    is SyncHeadline.UpToDate -> {
        val upToDate = headline.atMillis
            ?.let { stringResource(MR.strings.sync_status_up_to_date_at, relativeTimeSpanString(it)) }
            ?: stringResource(MR.strings.sync_status_up_to_date)
        when (val files = headline.files) {
            null -> upToDate
            is SyncHeadline.FilesNote.WaitingForWifi -> stringResource(
                MR.strings.sync_status_with_note,
                upToDate,
                pluralStringResource(MR.plurals.sync_files_waiting_for_wifi, files.count.toInt(), files.count.toInt()),
            )
            is SyncHeadline.FilesNote.MissingOnServer -> stringResource(
                MR.strings.sync_status_with_note,
                upToDate,
                pluralStringResource(MR.plurals.sync_files_missing, files.count.toInt(), files.count.toInt()),
            )
        }
    }
}

/** The colour role of a Sync row's subtitle: only lasting trouble is coloured; being offline is normal. */
fun SyncHeadline.tone(): ContentTone = when (this) {
    is SyncHeadline.Removed, is SyncHeadline.AccountDisabled, is SyncHeadline.UnreachableLong -> ContentTone.Error
    SyncHeadline.SignInAgain, SyncHeadline.AppTooOld, is SyncHeadline.ServerTooOld, SyncHeadline.ChoicesWaiting,
    is SyncHeadline.Questions, is SyncHeadline.HeldKinds, is SyncHeadline.Unsent,
    SyncHeadline.FirstDownloadWaitsForWifi,
    -> ContentTone.Warning
    is SyncHeadline.Downloading, SyncHeadline.Syncing -> ContentTone.Primary
    else -> ContentTone.Inherit
}

fun SyncHeadline.icon(): ImageVector = when (this) {
    SyncHeadline.NotSetUp -> MaterialSymbols.Rounded.CloudSync
    is SyncHeadline.Removed, is SyncHeadline.AccountDisabled -> MaterialSymbols.Rounded.PhonelinkErase
    SyncHeadline.SignInAgain, SyncHeadline.AppTooOld, is SyncHeadline.ServerTooOld, SyncHeadline.ChoicesWaiting,
    is SyncHeadline.Questions, is SyncHeadline.HeldKinds,
    -> MaterialSymbols.Rounded.SyncProblem
    is SyncHeadline.UnreachableLong, is SyncHeadline.Unsent, SyncHeadline.Offline, is SyncHeadline.Unreachable ->
        MaterialSymbols.Rounded.CloudOff
    is SyncHeadline.Downloading -> MaterialSymbols.Rounded.CloudDownload
    SyncHeadline.FirstDownloadWaitsForWifi -> MaterialSymbols.Rounded.Wifi
    SyncHeadline.Syncing -> MaterialSymbols.Rounded.Sync
    is SyncHeadline.UpToDate -> if (files is SyncHeadline.FilesNote.WaitingForWifi) {
        MaterialSymbols.Rounded.Wifi
    } else {
        MaterialSymbols.Rounded.CloudDone
    }
}
