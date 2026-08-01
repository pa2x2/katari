package eu.kanade.tachiyomi.ui.more

internal fun stripChecksumSection(changelogInfo: String): String {
    return changelogInfo.replace(CHECKSUM_SECTION_REGEX, "")
}

private val CHECKSUM_SECTION_REGEX = """(?ms)\R---[ \t]*\R+[ \t]*## Checksums[ \t]*\R.*\z""".toRegex()
