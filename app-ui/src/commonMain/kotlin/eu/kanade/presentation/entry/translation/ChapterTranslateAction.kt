package eu.kanade.presentation.entry.translation

import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateStatus
import tachiyomi.i18n.*

/** What a chapter's translation menu can ask for. */
enum class ChapterTranslateAction(val label: StringResource) {
    TRANSLATE(MR.strings.action_translate),
    TRANSLATE_AGAIN(MR.strings.action_translate_again),
    DOWNLOAD_AND_TRANSLATE(MR.strings.action_download_and_translate),
    TRANSLATE_NOW(MR.strings.action_start_translating_now),
    CANCEL(MR.strings.action_cancel_translation),
    RETRY(MR.strings.action_retry_translation),
    DELETE_TRANSLATION(MR.strings.action_delete_translation),
}

/**
 * The translation actions a chapter offers next to its download actions: translating a downloaded chapter, managing
 * one that is queued, translated or failed, and downloading and translating one that is not downloaded.
 */
fun chapterTranslateActions(
    downloadState: EntryDownloadState,
    status: EntryTranslateStatus?,
): List<ChapterTranslateAction> {
    val downloaded = downloadState == EntryDownloadState.DOWNLOADED
    val downloading = downloadState == EntryDownloadState.QUEUE || downloadState == EntryDownloadState.DOWNLOADING
    return when (status) {
        null, EntryTranslateStatus.Translated -> when {
            downloaded && status == EntryTranslateStatus.Translated -> listOf(
                ChapterTranslateAction.TRANSLATE_AGAIN,
                ChapterTranslateAction.DELETE_TRANSLATION,
            )
            downloaded -> listOf(ChapterTranslateAction.TRANSLATE)
            downloading -> emptyList()
            else -> listOf(ChapterTranslateAction.DOWNLOAD_AND_TRANSLATE)
        }
        EntryTranslateStatus.Queued -> listOf(ChapterTranslateAction.TRANSLATE_NOW, ChapterTranslateAction.CANCEL)
        EntryTranslateStatus.WaitingForDownload, is EntryTranslateStatus.Translating ->
            listOf(ChapterTranslateAction.CANCEL)
        is EntryTranslateStatus.Failed -> listOf(ChapterTranslateAction.RETRY, ChapterTranslateAction.CANCEL)
    }
}
