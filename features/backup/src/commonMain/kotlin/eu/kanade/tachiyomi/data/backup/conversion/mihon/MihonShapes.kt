package eu.kanade.tachiyomi.data.backup.conversion.mihon

import eu.kanade.tachiyomi.data.backup.conversion.newer.MessageShape

/*
 * What Katari knows of the Mihon family's messages beyond the fields it reads: the fields COVERAGE.md marks ignored, by
 * message, and trackers, which are left behind whole. A field outside these and the models is a newer app's (D123).
 */

/** An entry at 1. */
internal fun mihonEntryShape() = MessageShape(
    readAs = listOf(MihonManga.serializer().descriptor),
    ignored = MIHON_IGNORED,
    notRead = setOf(MihonTracking.serializer().descriptor),
)

/** The backup without its entries. */
internal fun mihonBackupShape() = MessageShape(
    readAs = listOf(MihonBackup.serializer().descriptor),
    // J2K's and older apps' broken sources at 100
    ignored = MIHON_IGNORED + (MihonBackup.serializer().descriptor to setOf(100)),
)

internal val MIHON_IGNORED = mapOf(
    // J2K's and Yōkai's broken history at 102; lastModifiedAt and version at 106 and 109
    MihonManga.serializer().descriptor to setOf(102, 106, 109),
    // lastModifiedAt and version at 11 and 12; J2K's and Yōkai's pages left at 800
    MihonChapter.serializer().descriptor to setOf(11, 12, 800),
    // TachiyomiSY's and Suwayomi's version, uid and lastModifiedAt
    MihonCategory.serializer().descriptor to setOf(601, 602, 603),
    // Komikku's merge address
    MihonMergeReference.serializer().descriptor to setOf(6),
)
