package mihon.feature.appupdate.check

/**
 * A version as the release workflow tags it: `X.Y.Z` or `X.Y.Z-prerelease`, with an optional leading `v`, ordered by
 * SemVer 2.0 precedence. Build metadata (`+...`) is accepted and ignored.
 */
internal data class ReleaseVersion(
    val major: Long,
    val minor: Long,
    val patch: Long,
    /** Dot-separated pre-release identifiers; empty for a stable release. */
    val prerelease: List<String>,
) : Comparable<ReleaseVersion> {

    val isPrerelease: Boolean get() = prerelease.isNotEmpty()

    override fun compareTo(other: ReleaseVersion): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        // A release outranks every pre-release of the same core version.
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return compareValues(other.prerelease.size, prerelease.size)
        }
        prerelease.zip(other.prerelease).forEach { (mine, theirs) ->
            compareIdentifiers(mine, theirs).let { if (it != 0) return it }
        }
        return compareValues(prerelease.size, other.prerelease.size)
    }

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

        fun parse(input: String): ReleaseVersion? {
            val match = PATTERN.matchEntire(input.trim()) ?: return null
            val (major, minor, patch, prerelease) = match.destructured
            return ReleaseVersion(
                major = major.toLongOrNull() ?: return null,
                minor = minor.toLongOrNull() ?: return null,
                patch = patch.toLongOrNull() ?: return null,
                prerelease = prerelease.takeIf { it.isNotEmpty() }?.split('.').orEmpty(),
            )
        }
    }
}

/** Numeric identifiers compare numerically and sort before alphanumeric ones, which compare as ASCII. */
private fun compareIdentifiers(a: String, b: String): Int {
    val aNumeric = a.all(Char::isDigit)
    val bNumeric = b.all(Char::isDigit)
    return when {
        // Compared without parsing, so identifiers of any length keep their order.
        aNumeric && bNumeric -> {
            val trimmedA = a.trimStart('0')
            val trimmedB = b.trimStart('0')
            compareValues(trimmedA.length, trimmedB.length).takeIf { it != 0 } ?: trimmedA.compareTo(trimmedB)
        }
        aNumeric -> -1
        bNumeric -> 1
        else -> a.compareTo(b)
    }
}
