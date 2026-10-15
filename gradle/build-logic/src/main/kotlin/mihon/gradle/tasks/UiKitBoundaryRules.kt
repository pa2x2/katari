package mihon.gradle.tasks

/*
 * presentation-core is the UI kit: the only module that may use Material components or decide what UI looks like.
 * Everywhere else, Compose code builds screens from kit components and kit tokens, so a raw Material widget, a dp
 * or sp literal, a colour, a text style, a corner shape or an alpha written outside the kit fails the build.
 * Generated icon modules are not UI code, and tests may build whatever harness they need.
 */

internal data class UiKitBoundarySource(val relativePath: String, val content: String)

internal data class UiKitBoundaryFinding(val relativePath: String, val lineNumber: Int?, val reason: String)

internal fun checkUiKitBoundaries(
    kotlinSources: List<UiKitBoundarySource>,
    buildScripts: List<UiKitBoundarySource>,
): List<UiKitBoundaryFinding> {
    val findings = mutableListOf<UiKitBoundaryFinding>()
    buildScripts
        .filter { script -> script.relativePath.isUiKitConsumerBuildScript() }
        .forEach { script ->
            script.content.lineSequence().forEachIndexed { index, line ->
                if (MATERIAL_BUILD_REFERENCE.containsMatchIn(line.substringBefore("//"))) {
                    findings += UiKitBoundaryFinding(
                        script.relativePath,
                        index + 1,
                        "Only presentation-core may depend on or opt into Compose Material; use the kit instead",
                    )
                }
            }
        }
    kotlinSources
        .filter { source -> source.relativePath.isUiKitConsumerSource() }
        .forEach { source ->
            val code = source.content.withoutCommentsAndStrings()
            val compose = COMPOSE_IMPORT.containsMatchIn(code)
            code.lineSequence().forEachIndexed { index, line ->
                UI_KIT_RULES
                    .filter { rule -> compose || !rule.composeOnly }
                    .filter { rule -> rule.matches(line) }
                    .forEach { rule -> findings += UiKitBoundaryFinding(source.relativePath, index + 1, rule.reason) }
            }
        }
    return findings
}

private class UiKitRule(
    val reason: String,
    private val pattern: Regex,
    val composeOnly: Boolean = true,
    private val accept: (MatchResult) -> Boolean = { true },
) {
    fun matches(line: String): Boolean = pattern.findAll(line).any(accept)
}

private val UI_KIT_RULES = listOf(
    UiKitRule(
        "Material components are only used inside presentation-core; use the kit component",
        Regex("""\bandroidx\.compose\.material3?\."""),
        composeOnly = false,
    ),
    UiKitRule(
        "Raw dialogs and popups bypass the kit; use the kit's Dialog, Sheet or menu",
        Regex("""^\s*import\s+androidx\.compose\.ui\.window\.(Dialog|Popup)\b"""),
        composeOnly = false,
    ),
    UiKitRule(
        "Dimensions come from kit tokens (KatariTheme.spacing, sizes) or kit component parameters",
        Regex("""(?<![\w.])(\d+(?:\.\d+)?)f?\.(dp|sp)\b"""),
        accept = { match -> match.groupValues[1].toDouble() != 0.0 },
    ),
    UiKitRule(
        "Colours come from KatariTheme.colors roles",
        Regex("""\bColor\s*\(|\bColor\.(?!Transparent\b|Unspecified\b)\w+"""),
    ),
    UiKitRule(
        "Text styling comes from kit text roles",
        Regex("""\b(TextStyle|SpanStyle|ParagraphStyle)\s*\(|\b(FontWeight|FontStyle)\.|\bfontWeight\s*="""),
    ),
    UiKitRule(
        "Corner shapes come from KatariTheme.shapes roles",
        Regex("""\b(RoundedCornerShape|AbsoluteRoundedCornerShape|CutCornerShape)\s*\(|\bCircleShape\b"""),
    ),
    UiKitRule(
        "Emphasis comes from colour roles and enabled states, not alpha",
        Regex("""\.copy\(\s*alpha\s*=|\balpha\s*\(\s*0?\.\d|\balpha\s*=\s*0?\.\d"""),
    ),
)

private val COMPOSE_IMPORT = Regex("""(?m)^\s*import\s+androidx\.(compose|glance)\.""")
private val MATERIAL_BUILD_REFERENCE =
    Regex("""\blibs\.androidx\.compose\.material3?\b(?!\w)|\bandroidx\.compose\.material3?\.""")

private fun String.isUiKitConsumerBuildScript(): Boolean {
    val module = substringBeforeLast("/build.gradle.kts", missingDelimiterValue = "")
    return endsWith("build.gradle.kts") && module.isNotEmpty() && module.isUiKitConsumerModule()
}

private fun String.isUiKitConsumerSource(): Boolean {
    if (!endsWith(".kt")) return false
    val module = substringBefore("/src/", missingDelimiterValue = "")
    val sourceSet = substringAfter("/src/", missingDelimiterValue = "").substringBefore("/")
    return module.isNotEmpty() && module.isUiKitConsumerModule() && !sourceSet.contains("test", ignoreCase = true)
}

private fun String.isUiKitConsumerModule(): Boolean {
    return this != "presentation-core" && this != "icons" && !startsWith("icons/") && !startsWith("gradle/")
}

/** Blanks comments and string literal contents while keeping line breaks, so findings keep their line numbers. */
private fun String.withoutCommentsAndStrings(): String {
    val out = StringBuilder(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        when {
            startsWith("//", index) -> {
                while (index < length && this[index] != '\n') {
                    out.append(' ')
                    index++
                }
            }
            startsWith("/*", index) -> {
                val end = indexOf("*/", index + 2).let { if (it < 0) length else it + 2 }
                while (index < end) {
                    out.append(if (this[index] == '\n') '\n' else ' ')
                    index++
                }
            }
            startsWith("\"\"\"", index) -> {
                val end = indexOf("\"\"\"", index + 3).let { if (it < 0) length else it + 3 }
                while (index < end) {
                    out.append(if (this[index] == '\n') '\n' else ' ')
                    index++
                }
            }
            char == '"' || char == '\'' -> {
                out.append(' ')
                index++
                while (index < length && this[index] != char && this[index] != '\n') {
                    if (this[index] == '\\') {
                        out.append(' ')
                        index++
                    }
                    if (index < length) {
                        out.append(' ')
                        index++
                    }
                }
                if (index < length && this[index] == char) {
                    out.append(' ')
                    index++
                }
            }
            else -> {
                out.append(char)
                index++
            }
        }
    }
    return out.toString()
}
