package app.katari.sync.ui.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudDone
import app.katari.icons.materialsymbols.rounded.CloudDownload
import app.katari.icons.materialsymbols.rounded.CloudOff
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.icons.materialsymbols.rounded.CloudUpload
import app.katari.icons.materialsymbols.rounded.Sync
import app.katari.icons.materialsymbols.rounded.SyncDisabled
import app.katari.icons.materialsymbols.rounded.SyncProblem
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sync.client.run.SyncStep
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.relativeTimeSpanString
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
    SyncHeadline.ServerReset -> stringResource(MR.strings.sync_status_server_reset)
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
    is SyncHeadline.HeldAside ->
        pluralStringResource(MR.plurals.sync_status_held_aside, headline.count.toInt(), headline.count.toInt())
    is SyncHeadline.Downloading ->
        headline.percent
            ?.let { stringResource(MR.strings.sync_status_downloading, it) }
            ?: stringResource(MR.strings.sync_status_downloading_unknown)
    is SyncHeadline.CatchingUp ->
        headline.percent
            ?.let { stringResource(MR.strings.sync_status_catching_up, it) }
            ?: stringResource(MR.strings.sync_status_catching_up_unknown)
    is SyncHeadline.Step -> stepText(headline.step)
    SyncHeadline.FirstDownloadWaitsForWifi -> stringResource(platformPresentation.syncFirstDownloadWaitsLabel)
    is SyncHeadline.Paused -> unsentText(headline.unsent, MR.strings.sync_unsent_paused)
        ?: stringResource(MR.strings.sync_status_paused)
    is SyncHeadline.ChangesWaitForWifi -> unsentText(headline.unsent, platformPresentation.syncUnsentWaitingNote)
        ?: stringResource(platformPresentation.syncChangesWaitLabel)
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
    is SyncHeadline.Failed ->
        headline.lastSyncedAtMillis
            ?.let { stringResource(MR.strings.sync_status_failed_at, relativeTimeSpanString(it)) }
            ?: stringResource(MR.strings.sync_status_failed)
    is SyncHeadline.UpToDate -> {
        val upToDate = headline.atMillis
            ?.let { stringResource(MR.strings.sync_status_up_to_date_at, relativeTimeSpanString(it)) }
            ?: stringResource(MR.strings.sync_status_up_to_date)
        when (val files = headline.files) {
            null -> upToDate
            is SyncHeadline.FilesNote.WaitingForWifi -> stringResource(
                MR.strings.sync_status_with_note,
                upToDate,
                pluralStringResource(
                    platformPresentation.syncFilesWaitingPlural,
                    files.count.toInt(),
                    files.count.toInt(),
                ),
            )
            is SyncHeadline.FilesNote.MissingOnServer -> stringResource(
                MR.strings.sync_status_with_note,
                upToDate,
                pluralStringResource(MR.plurals.sync_files_missing, files.count.toInt(), files.count.toInt()),
            )
        }
    }
}

/** [count] changes not sent yet, with why; null when there are none. */
@Composable
private fun unsentText(count: Long, because: StringResource): String? {
    if (count <= 0) return null
    return stringResource(
        MR.strings.sync_status_with_note,
        pluralStringResource(MR.plurals.sync_unsent, count.toInt(), count.toInt()),
        stringResource(because),
    )
}

@Composable
private fun stepText(step: SyncStep): String {
    val done = step.done.toInt()
    val total = step.total.toInt()
    val percent = if (step.total > 0) (step.done * 100 / step.total).toInt() else 0
    return when (step.kind) {
        SyncStep.Kind.PREPARING -> stringResource(MR.strings.sync_status_preparing, percent)
        SyncStep.Kind.UPLOADING_RECORDS -> stringResource(MR.strings.sync_status_uploading, percent)
        SyncStep.Kind.UPLOADING_FILES -> stringResource(MR.strings.sync_status_uploading_files, done, total)
        SyncStep.Kind.DOWNLOADING_FILES -> stringResource(MR.strings.sync_status_downloading_files, done, total)
        SyncStep.Kind.DOWNLOADING_CHANGES -> stringResource(MR.strings.sync_status_downloading_changes, percent)
    }
}

/** The colour role of a Sync row's subtitle: only lasting trouble is coloured; being offline is normal. */
fun SyncHeadline.tone(): ContentTone = when (this) {
    is SyncHeadline.Removed, is SyncHeadline.AccountDisabled, is SyncHeadline.UnreachableLong, is SyncHeadline.Failed,
    -> ContentTone.Error
    SyncHeadline.SignInAgain, SyncHeadline.ServerReset, SyncHeadline.AppTooOld, is SyncHeadline.ServerTooOld,
    SyncHeadline.ChoicesWaiting, is SyncHeadline.Questions, is SyncHeadline.HeldKinds, is SyncHeadline.HeldAside,
    is SyncHeadline.Unsent, SyncHeadline.FirstDownloadWaitsForWifi, is SyncHeadline.ChangesWaitForWifi,
    -> ContentTone.Warning
    is SyncHeadline.Downloading, is SyncHeadline.CatchingUp, is SyncHeadline.Step, SyncHeadline.Syncing ->
        ContentTone.Primary
    else -> ContentTone.Inherit
}

/** Whether sync is stopped until the person, an update or time does something, which the status card's icon marks. */
fun SyncHeadline.stopsSync(): Boolean = when (this) {
    is SyncHeadline.Removed, is SyncHeadline.AccountDisabled, is SyncHeadline.UnreachableLong, SyncHeadline.SignInAgain,
    SyncHeadline.ServerReset, SyncHeadline.AppTooOld, is SyncHeadline.ServerTooOld,
    -> true
    else -> false
}

fun SyncHeadline.icon(): ImageVector = when (this) {
    SyncHeadline.NotSetUp -> MaterialSymbols.Rounded.CloudSync
    is SyncHeadline.Removed, is SyncHeadline.AccountDisabled -> platformPresentation.thisDeviceRemovedIcon
    SyncHeadline.SignInAgain, SyncHeadline.ServerReset, SyncHeadline.AppTooOld, is SyncHeadline.ServerTooOld,
    SyncHeadline.ChoicesWaiting, is SyncHeadline.Questions, is SyncHeadline.HeldKinds, is SyncHeadline.HeldAside,
    is SyncHeadline.Failed,
    -> MaterialSymbols.Rounded.SyncProblem
    is SyncHeadline.UnreachableLong, is SyncHeadline.Unsent, SyncHeadline.Offline, is SyncHeadline.Unreachable ->
        MaterialSymbols.Rounded.CloudOff
    is SyncHeadline.Downloading -> MaterialSymbols.Rounded.CloudDownload
    is SyncHeadline.CatchingUp -> MaterialSymbols.Rounded.CloudSync
    is SyncHeadline.Step -> when (step.kind) {
        SyncStep.Kind.PREPARING -> MaterialSymbols.Rounded.CloudSync
        SyncStep.Kind.UPLOADING_RECORDS, SyncStep.Kind.UPLOADING_FILES -> MaterialSymbols.Rounded.CloudUpload
        SyncStep.Kind.DOWNLOADING_FILES, SyncStep.Kind.DOWNLOADING_CHANGES -> MaterialSymbols.Rounded.CloudDownload
    }
    SyncHeadline.FirstDownloadWaitsForWifi, is SyncHeadline.ChangesWaitForWifi ->
        platformPresentation.unmeteredNetworkIcon
    is SyncHeadline.Paused -> MaterialSymbols.Rounded.SyncDisabled
    SyncHeadline.Syncing -> MaterialSymbols.Rounded.Sync
    is SyncHeadline.UpToDate -> if (files is SyncHeadline.FilesNote.WaitingForWifi) {
        platformPresentation.unmeteredNetworkIcon
    } else {
        MaterialSymbols.Rounded.CloudDone
    }
}
