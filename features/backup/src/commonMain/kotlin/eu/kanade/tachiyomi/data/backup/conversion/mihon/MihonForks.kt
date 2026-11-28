package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents

/**
 * The fork of the Mihon family whose own parts a backup has (D109, research/10 section 2), or null when it has none or
 * those of more than one. Each sign is a field only that fork writes, so it shows only where the person used what it
 * holds, apart from TachiyomiSY's category ids, which it writes for every category. TachiyomiJ2K and Yōkai share every
 * field, and Tachiyomi has none of its own, so none of them is named.
 */
internal fun mihonFork(contents: BackupFileContents.Protobuf): String? {
    val root = contents.fieldNumbers
    // Suwayomi shares TachiyomiSY's category fields, so its own root fields decide first
    if (root.any { it in SUWAYOMI_ROOT }) return SUWAYOMI
    val entries = contents.innerFieldNumbers[ENTRIES].orEmpty()
    val categories = contents.innerFieldNumbers[CATEGORIES].orEmpty()
    val forks = buildSet {
        // Feeds at 610 and hidden categories
        if (KOMIKKU_FEEDS in root || KOMIKKU_HIDDEN in categories) add(KOMIKKU)
        // Category version, id and change time; feeds at 601 and merge settings in later builds
        if (categories.any { it in SY_CATEGORY } || SY_FEEDS in root || SY_MERGE_SETTINGS in entries) add(SY)
    }
    return forks.singleOrNull()
}

private const val ENTRIES = 1
private const val CATEGORIES = 2

private const val SUWAYOMI = "Suwayomi"
private val SUWAYOMI_ROOT = 9000..9002

private const val KOMIKKU = "Komikku"
private const val KOMIKKU_FEEDS = 610
private const val KOMIKKU_HIDDEN = 900

private const val SY = "TachiyomiSY"
private val SY_CATEGORY = 601..603
private const val SY_FEEDS = 601
private const val SY_MERGE_SETTINGS = 604
