package eu.kanade.tachiyomi.ui.more

internal fun sanitizeInAppReleaseNotes(changelogInfo: String): String {
    return changelogInfo
        .replace(CHECKSUM_SECTION_REGEX, "")
        .replace(DOWNLOAD_SELECTION_TIP_REGEX, "")
}

private val CHECKSUM_SECTION_REGEX = """(?ms)\R---[ \t]*\R+[ \t]*## Checksums[ \t]*\R.*\z""".toRegex()
private val DOWNLOAD_SELECTION_TIP_REGEX =
    """(?im)\R{2,}>[ \t]*\[!TIP][ \t]*\R(?:>[ \t]*\R)+>[ \t]*If you are unsure which version to download, use `[^`\r\n]+\.apk`\.[ \t]*(?=\R*\z)"""
        .toRegex()
