package app.katari.sync.ui.notice

import app.katari.sync.client.run.SyncStep
import app.katari.sync.client.status.SyncStatus
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/**
 * What the notification of a long run says, such as "Uploading your library" over "127.0.0.1 · 42%": [title]
 * for what it is doing, [server] it syncs with and [percent] it got through, each null while unknown.
 */
data class SyncLongRunNotice(val title: StringResource, val server: String?, val percent: Int?) {
    companion object {
        /** The notice for [status], or for a long run that hasn't reported yet when it is null. */
        fun of(status: SyncStatus?): SyncLongRunNotice {
            val server = (status?.link as? SyncStatus.Link.Linked)?.server?.host
            val syncing = status?.activity as? SyncStatus.Activity.Syncing
            val step = syncing?.step
            return when {
                syncing?.catchingUp == true ->
                    SyncLongRunNotice(MR.strings.sync_long_run_catching_up, server, syncing.percent)
                syncing?.percent != null ->
                    SyncLongRunNotice(MR.strings.sync_long_run_downloading, server, syncing.percent)
                step != null -> SyncLongRunNotice(step.kind.title(), server, step.percent())
                else -> SyncLongRunNotice(MR.strings.sync_long_run_syncing, server, null)
            }
        }

        // The files a long run moves belong to the library it uploads or downloads
        private fun SyncStep.Kind.title(): StringResource = when (this) {
            SyncStep.Kind.PREPARING -> MR.strings.sync_long_run_preparing
            SyncStep.Kind.UPLOADING_RECORDS, SyncStep.Kind.UPLOADING_FILES -> MR.strings.sync_long_run_uploading
            SyncStep.Kind.DOWNLOADING_CHANGES, SyncStep.Kind.DOWNLOADING_FILES ->
                MR.strings.sync_long_run_downloading
        }

        private fun SyncStep.percent(): Int = if (total > 0) (done * 100 / total).toInt() else 0
    }
}
