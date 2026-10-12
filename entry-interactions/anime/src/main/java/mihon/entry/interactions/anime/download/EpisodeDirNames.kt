package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil

/**
 * The names an episode's download folder is written and found under: the sanitized episode name cut to fit, then a
 * hash of the episode URL unless that is turned off. Lookups also accept the names either value of the non-ASCII and
 * hash settings wrote, and the unhashed, uncut name from before the hash existed, so changing a naming setting never
 * loses a download. Each part is computed once however many names are built from it.
 */
internal class EpisodeDirNames(episodeName: String, private val url: String) {

    private val episodeName = episodeName.ifBlank { "Episode" }

    private val hashSuffix by lazy(LazyThreadSafetyMode.NONE) { "_" + md5(url).take(6) }

    private val cutEpisodeNames = arrayOfNulls<String>(2)

    fun dirName(disallowNonAscii: Boolean, appendHash: Boolean): String {
        return cutEpisodeName(disallowNonAscii) + if (appendHash) hashSuffix else ""
    }

    /** The name a new download gets comes first. */
    fun validDirNames(disallowNonAscii: Boolean, appendHash: Boolean): List<String> {
        return buildSet {
            add(dirName(disallowNonAscii, appendHash))
            add(DiskUtil.buildValidFilename(episodeName))
            for (otherDisallowNonAscii in BOOLEANS) {
                for (otherAppendHash in BOOLEANS) {
                    add(dirName(otherDisallowNonAscii, otherAppendHash))
                }
            }
        }
            .toList()
    }

    private fun cutEpisodeName(disallowNonAscii: Boolean): String {
        val index = if (disallowNonAscii) 1 else 0
        return cutEpisodeNames[index]
            ?: DiskUtil.buildValidFilename(episodeName, MAX_NAME_BYTES, disallowNonAscii)
                .also { cutEpisodeNames[index] = it }
    }

    private companion object {
        // Leaves room for the hash and its underscore
        const val MAX_NAME_BYTES = DiskUtil.MAX_FILE_NAME_BYTES - 8

        val BOOLEANS = booleanArrayOf(false, true)
    }
}
