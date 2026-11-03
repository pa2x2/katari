package eu.kanade.tachiyomi.ui.entry.cover

import android.app.Application
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.source.local.image.LocalCoverManager
import java.io.InputStream

/** Reads the picked image through the content resolver; a local entry's cover is written by the local source. */
@Inject
class AndroidEntryCoverDocuments(
    private val application: Application,
    private val localCoverManager: LocalCoverManager,
) : EntryCoverDocuments {
    override val editsLocalCovers: Boolean = true

    override fun open(document: DocumentUri): InputStream? =
        application.contentResolver.openInputStream(document.value.toUri())

    override fun replaceLocalCover(entry: Entry, image: InputStream) {
        localCoverManager.update(entry.url, image)
    }
}
