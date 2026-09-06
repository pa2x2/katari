package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.UpdateStrategy

fun UpdateStrategy.toEntryUpdateStrategy(): EntryUpdateStrategy = when (this) {
    UpdateStrategy.ALWAYS_UPDATE -> EntryUpdateStrategy.ALWAYS_UPDATE
    UpdateStrategy.ONLY_FETCH_ONCE -> EntryUpdateStrategy.ONLY_FETCH_ONCE
}

fun EntryUpdateStrategy.toLegacyUpdateStrategy(): UpdateStrategy = when (this) {
    EntryUpdateStrategy.ALWAYS_UPDATE -> UpdateStrategy.ALWAYS_UPDATE
    EntryUpdateStrategy.ONLY_FETCH_ONCE -> UpdateStrategy.ONLY_FETCH_ONCE
}

fun Page.asEntryImagePage(): EntryImagePage =
    EntryImagePage(
        index = index,
        url = url,
        imageUrl = imageUrl,
    )

fun EntryImagePage.toLegacyPage(): Page =
    Page(
        index = index,
        url = url,
        imageUrl = imageUrl,
    )

fun EntryImagePage.toLegacyPage(progress: ProgressListener?): Page {
    if (progress == null) {
        return toLegacyPage()
    }
    return object : Page(index, url, imageUrl) {
        override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
            super.update(bytesRead, contentLength, done)
            progress.update(bytesRead, contentLength, done)
        }
    }
}
