package mihon.entry.interactions.manga.download

import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil

/**
 * The names a chapter's download, a folder of images or the same name with `.cbz`, is written and found under.
 *
 * A download is named `<scanlator>_<chapter name>`, sanitized and cut to fit as one string so a long or non-ASCII
 * scanlator stays within the name limit and the non-ASCII setting, then a hash of the chapter URL unless that is turned
 * off. Lookups also accept every name an earlier setting or app version wrote, so neither changing a naming setting
 * nor updating loses a download:
 * - either value of the non-ASCII and hash settings,
 * - the unhashed, uncut name from before the hash existed,
 * - the names Mihon 0.21 writes, which sanitize the scanlator on its own and cut only the chapter name, and the ones
 *   its preview builds wrote with the scanlator unsanitized.
 *
 * Each part is computed once however many names are built from it.
 */
internal class ChapterDirNames(chapterName: String, private val scanlator: String?, private val url: String) {

    private val chapterName = chapterName.ifBlank { "Chapter" }

    private val hasScanlator = !scanlator.isNullOrBlank()

    private val joinedName = if (hasScanlator) "${scanlator}_${this.chapterName}" else this.chapterName

    private val hashSuffix by lazy(LazyThreadSafetyMode.NONE) { "_" + md5(url).take(6) }

    private val sanitizedScanlator by lazy(LazyThreadSafetyMode.NONE) {
        DiskUtil.buildValidFilename(scanlator.orEmpty())
    }

    private val joinedNames = arrayOfNulls<String>(2)

    private val cutChapterNames = arrayOfNulls<String>(2)

    fun dirName(disallowNonAscii: Boolean, appendHash: Boolean): String {
        return joinedName(disallowNonAscii) + suffix(appendHash)
    }

    /** The name a new download gets comes first, each name followed by its archive. */
    fun validDirNames(disallowNonAscii: Boolean, appendHash: Boolean): List<String> {
        val current = dirName(disallowNonAscii, appendHash)
        val legacy = buildSet {
            add(DiskUtil.buildValidFilename(joinedName))
            for (otherDisallowNonAscii in BOOLEANS) {
                for (otherAppendHash in BOOLEANS) {
                    val suffix = suffix(otherAppendHash)
                    add(joinedName(otherDisallowNonAscii) + suffix)
                    add(mihonName(otherDisallowNonAscii, sanitizeScanlator = true) + suffix)
                    // One with a "/" nested its folder, so it was never found anyway
                    if (scanlator != null && '/' !in scanlator) {
                        add(mihonName(otherDisallowNonAscii, sanitizeScanlator = false) + suffix)
                    }
                }
            }
            remove(current)
        }

        return buildList {
            add(current)
            add("$current.cbz")
            legacy.forEach {
                add(it)
                add("$it.cbz")
            }
        }
    }

    private fun suffix(appendHash: Boolean) = if (appendHash) hashSuffix else ""

    private fun joinedName(disallowNonAscii: Boolean): String {
        val index = if (disallowNonAscii) 1 else 0
        return joinedNames[index]
            ?: DiskUtil.buildValidFilename(joinedName, MAX_NAME_BYTES, disallowNonAscii)
                .also { joinedNames[index] = it }
    }

    private fun mihonName(disallowNonAscii: Boolean, sanitizeScanlator: Boolean): String {
        val index = if (disallowNonAscii) 1 else 0
        val cutChapterName = cutChapterNames[index]
            ?: DiskUtil.buildValidFilename(chapterName, MAX_NAME_BYTES, disallowNonAscii)
                .also { cutChapterNames[index] = it }
        val scanlatorPart = when {
            !hasScanlator -> return cutChapterName
            sanitizeScanlator -> sanitizedScanlator
            else -> scanlator
        }
        return "${scanlatorPart}_$cutChapterName"
    }

    private companion object {
        // Leaves 7 bytes for the hash and its underscore, 4 bytes for .cbz
        const val MAX_NAME_BYTES = DiskUtil.MAX_FILE_NAME_BYTES - 11

        val BOOLEANS = booleanArrayOf(false, true)
    }
}
