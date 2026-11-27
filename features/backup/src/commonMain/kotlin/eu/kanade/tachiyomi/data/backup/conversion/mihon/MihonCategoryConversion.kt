package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.*

internal fun MihonCategory.toBackupCategory(): BackupCategory = BackupCategory(
    name = name,
    order = order,
    id = id,
    flags = j2kSort?.let(::j2kLibrarySort)?.let { it.type.flag or it.direction.flag } ?: flags,
)

/** The report's line for the category: a Komikku hidden one is shown in Katari (D031), a sort Katari lacks noted. */
internal fun MihonCategory.reportItem(): Item = when {
    hidden -> Item(name, MR.strings.backup_conversion_hidden_category)
    j2kSort != null && j2kLibrarySort(j2kSort) == null -> Item(name, MR.strings.backup_conversion_manual_category_sort)
    else -> Item(name)
}

/**
 * TachiyomiJ2K's and Yōkai's category sort (`LibrarySort.categoryValue` in TachiyomiJ2K, Jays2Kings/tachiyomiJ2K
 * master): `'a' + 2 * n` ascending and `'b' + 2 * n` descending for its n-th sort, and `'D'` for a manual order, which
 * Katari has no counterpart for (D039).
 */
private fun j2kLibrarySort(char: Int): LibrarySort? {
    val offset = char - 'a'.code
    if (offset < 0) return null
    val type = when (offset / 2) {
        0 -> LibrarySort.Type.Alphabetical
        1 -> LibrarySort.Type.LastRead
        2 -> LibrarySort.Type.LatestChapter
        3 -> LibrarySort.Type.UnreadCount
        4 -> LibrarySort.Type.TotalChapters
        5 -> LibrarySort.Type.DateAdded
        6 -> LibrarySort.Type.ChapterFetchDate
        else -> return null
    }
    val direction = if (offset % 2 == 0) LibrarySort.Direction.Ascending else LibrarySort.Direction.Descending
    return LibrarySort(type, direction)
}
