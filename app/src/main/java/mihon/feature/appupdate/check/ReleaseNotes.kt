package mihon.feature.appupdate.check

/**
 * A GitHub release body made fit for the app: GitHub alert blocks (`> [!TIP]` and the like) are dropped, since the
 * release workflow uses them for download instructions aimed at the releases page and the app's Markdown renderer
 * doesn't draw them, and `@user` mentions become profile links as GitHub renders them.
 */
internal fun cleanReleaseNotes(body: String): String {
    val kept = mutableListOf<String>()
    var inAlert = false
    for (line in body.replace("\r\n", "\n").lines()) {
        if (ALERT_START.containsMatchIn(line)) {
            inAlert = true
            continue
        }
        if (inAlert && line.startsWith(">")) continue
        inAlert = false
        kept += line
    }
    return kept.joinToString("\n")
        .replace(EXTRA_BLANK_LINES, "\n\n")
        .trim()
        .replace(USERNAME_MENTION) { mention -> "[${mention.value}](https://github.com/${mention.value.drop(1)})" }
}

private val ALERT_START = Regex("""^>\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]""", RegexOption.IGNORE_CASE)
private val EXTRA_BLANK_LINES = Regex("""\n{3,}""")

/**
 * A GitHub username mention as GitHub Flavored Markdown matches it: alphanumeric with single inner hyphens, at most 39
 * characters, not preceded by a word character (so e-mail addresses don't match).
 */
private val USERNAME_MENTION =
    Regex("""\B@([a-z0-9](?:-(?=[a-z0-9])|[a-z0-9]){0,38}(?<=[a-z0-9]))""", RegexOption.IGNORE_CASE)
