package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * LNReader's backup, as far as converting it needs: a zip of JSON files, laid out by src/services/backup/utils.ts and
 * the types of src/database/types/index.ts (LNReader/lnreader v2.1.4, a0c20e209c8b). Keys not declared here are
 * skipped.
 */

/** `Version.json`. Backups before format 2 have no `formatVersion`. */
@Serializable
internal class LnReaderVersion(val formatVersion: Int? = null)

/** An entry of `Category.json`; [novelIds] are LNReader's own novel ids. */
@Serializable
internal class LnReaderCategory(
    val id: Long,
    val name: String,
    val sort: Long = 0,
    val novelIds: List<Long> = emptyList(),
)

/** An entry of `Plugins.json`, the plugins installed; [url] is the plugin's own script, under its repository. */
@Serializable
internal class LnReaderPlugin(val id: String, val name: String = "", val url: String = "")

/** `NovelAndChapters/<id>.json`. */
@Serializable
internal class LnReaderNovel(
    val id: Long,
    val path: String,
    val pluginId: String,
    val name: String = "",
    val summary: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val status: String? = null,
    /** Comma-separated. */
    val genres: String? = null,
    val inLibrary: Boolean = false,
    /** An imported book, which belongs to no plugin. */
    val isLocal: Boolean = false,
    val chapters: List<LnReaderChapter> = emptyList(),
)

@Serializable
internal class LnReaderChapter(
    val path: String,
    val name: String = "",
    /** What the plugin sent: a time, an epoch number or the site's own text. */
    val releaseTime: JsonElement? = null,
    /** When it was last read, as local time without a zone ("2026-06-02 21:14:05"). */
    val readTime: String? = null,
    val bookmark: Boolean = false,
    val unread: Boolean = true,
    val chapterNumber: Double? = null,
    /** Its place in the list, counting from the oldest chapter at 0. */
    val position: Long = 0,
    /** How far it was read, in whole percent. */
    val progress: Int? = null,
    val scanlator: String? = null,
    /** How long it was read, in milliseconds. */
    val timeSpent: Long? = null,
)
