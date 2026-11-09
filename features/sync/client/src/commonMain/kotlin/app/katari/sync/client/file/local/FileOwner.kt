package app.katari.sync.client.file.local

import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.LocalRef
import app.katari.sync.protocol.file.FileCategory

/**
 * What a synced file belongs to on this device, by local IDs: an entry's custom cover, or a font a profile imported.
 * [ref] is the local ref of the record that names the file, so the owner and its record are found from each other.
 */
sealed interface FileOwner {
    val category: FileCategory
    val ref: LocalRef

    data class CustomCover(val entryId: Long) : FileOwner {
        override val category: FileCategory get() = FileCategory.COVER
        override val ref: LocalRef get() = LocalRef.of(entryId)
    }

    data class Font(val profileId: Long, val fileName: String) : FileOwner {
        override val category: FileCategory get() = FileCategory.FONT
        override val ref: LocalRef get() = LocalRef.of(profileId, fileName)
    }

    companion object {
        fun of(category: FileCategory, ref: LocalRef): FileOwner = when (category) {
            FileCategory.COVER -> CustomCover(ref.long(0))
            FileCategory.FONT -> Font(ref.long(0), ref.text(1))
        }
    }
}

/** How `sync_local_files` and `sync_file_uploads` name [FileCategory]. */
internal val FileCategory.stored: String
    get() = when (this) {
        FileCategory.COVER -> "cover"
        FileCategory.FONT -> "font"
    }

internal fun fileCategoryOf(stored: String): FileCategory =
    FileCategory.entries.first { it.stored == stored }

/** The data group whose switch decides whether this device syncs files of this category. */
internal val FileCategory.dataGroup: DataGroup
    get() = when (this) {
        FileCategory.COVER -> DataGroup.COVERS
        FileCategory.FONT -> DataGroup.FONTS
    }
