package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import eu.kanade.tachiyomi.data.backup.conversion.newer.JsonShape
import kotlinx.serialization.builtins.ListSerializer

/*
 * What Katari knows of LNReader's JSON files beyond the keys it reads: the rest of what LNReader 2.1.4 writes
 * (src/database/types/index.ts and src/services/backup/utils.ts at a0c20e209c8b, checked against L1). A key outside
 * these and the models is a newer LNReader's (D123).
 */

internal fun lnReaderVersionShape() = JsonShape(
    LnReaderVersion.serializer().descriptor,
    mapOf(LnReaderVersion.serializer().descriptor to setOf("sections")),
)

internal fun lnReaderCategoriesShape() = JsonShape(ListSerializer(LnReaderCategory.serializer()).descriptor)

internal fun lnReaderPluginsShape() = JsonShape(
    ListSerializer(LnReaderPlugin.serializer()).descriptor,
    mapOf(
        LnReaderPlugin.serializer().descriptor to
            setOf("hasSettings", "hasUpdate", "iconUrl", "lang", "site", "version"),
    ),
)

internal fun lnReaderNovelShape() = JsonShape(
    LnReaderNovel.serializer().descriptor,
    mapOf(
        LnReaderNovel.serializer().descriptor to setOf(
            "cover",
            "totalPages",
            "totalChapters",
            "chaptersDownloaded",
            "chaptersUnread",
            "lastReadAt",
            "lastUpdatedAt",
        ),
        LnReaderChapter.serializer().descriptor to setOf("id", "novelId", "isDownloaded", "updatedTime", "page"),
    ),
)
