package mihon.gradle.tasks

/*
 * presentation-core is the UI kit: the only module that may use Material components or decide what UI looks like.
 * Everywhere else, Compose code builds screens from kit components and kit tokens, so a raw Material widget, a dp
 * or sp literal, a colour, a text style, a corner shape, a shadow or an alpha written outside the kit fails the build.
 * The kit's own colour and shape scales are off limits too: outside the kit, a fill is picked by SurfaceContainer
 * role, content by ContentTone, and a rounded or raised container by the kit component that names its job.
 * Generated icon modules are not UI code, and tests may build whatever harness they need.
 *
 * The kit also owns how a screen adapts to the window (.agents/skills/adaptive-layout): outside it, nothing reads the
 * window's size or orientation, builds a raw vertical lazy list or picks a grid's columns. Readers and the player
 * are exempt from those rules until each is reviewed for them; see ADAPTIVE_LAYOUT_EXEMPT_PATHS.
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
            val rules = when {
                source.relativePath.isAdaptiveLayoutExempt() -> UI_KIT_RULES
                else -> UI_KIT_RULES + ADAPTIVE_LAYOUT_RULES
            }
            val facts = SourceFacts(CONFIGURATION_ALIAS.findAll(code).map { it.groupValues[1] }.toSet())
            code.lineSequence().forEachIndexed { index, line ->
                rules
                    .filter { rule -> compose || !rule.composeOnly }
                    .filter { rule -> rule.matches(line, facts) }
                    .forEach { rule -> findings += UiKitBoundaryFinding(source.relativePath, index + 1, rule.reason) }
            }
        }
    return findings
}

private class UiKitRule(
    val reason: String,
    private val pattern: Regex,
    val composeOnly: Boolean = true,
    private val accept: (MatchResult, SourceFacts) -> Boolean = { _, _ -> true },
) {
    fun matches(line: String, facts: SourceFacts): Boolean = pattern.findAll(line).any { accept(it, facts) }
}

/** What a rule may need to know about the whole file, beyond the line it matches. */
private class SourceFacts(val configurationNames: Set<String>)

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
        accept = { match, _ -> match.groupValues[1].toDouble() != 0.0 },
    ),
    UiKitRule(
        "Colours are picked by role: SurfaceContainer for fills, ContentTone for content",
        Regex("""\bColor\s*\(|\bColor\.(?!Transparent\b|Unspecified\b)\w+"""),
    ),
    UiKitRule(
        "Text styling comes from kit text roles",
        Regex("""\b(TextStyle|SpanStyle|ParagraphStyle)\s*\(|\b(FontWeight|FontStyle)\.|\bfontWeight\s*="""),
    ),
    UiKitRule(
        "Rounded containers come from kit components, such as Card, GroupCard or OverlayPill",
        Regex("""\b(RoundedCornerShape|AbsoluteRoundedCornerShape|CutCornerShape)\s*\(|\bCircleShape\b"""),
    ),
    UiKitRule(
        "Scheme colours stay in the kit; use SurfaceContainer, ContentTone, KatariTheme.chart or KatariTheme.markup",
        Regex("""\bKatariTheme\.colors\b"""),
    ),
    UiKitRule(
        "Shapes are the kit's; use the kit component for the container, such as Card, OverlayPanel or ArtworkTile",
        Regex("""\bKatariTheme\.shapes\b"""),
    ),
    UiKitRule(
        "Shadows come with raised kit components, such as GroupCard(raised = true) or OverlayStyle.Raised",
        Regex("""\.shadow\s*\("""),
    ),
    UiKitRule(
        "Emphasis comes from colour roles and enabled states, not alpha",
        Regex("""\.copy\(\s*alpha\s*=|\balpha\s*\(\s*0?\.\d|\balpha\s*=\s*0?\.\d"""),
    ),
)

private const val ADAPTIVE_LAYOUT_SKILL = "see .agents/skills/adaptive-layout"

private val ADAPTIVE_LAYOUT_RULES = listOf(
    UiKitRule(
        "Window size and orientation come from currentWindowLayout(), or a kit layout adapts to them; " +
            ADAPTIVE_LAYOUT_SKILL,
        Regex(
            """(\bLocalConfiguration\s*\.\s*current|\b\w+)\s*\??\.\s*""" +
                """(?:orientation|screenWidthDp|screenHeightDp|smallestScreenWidthDp)\b|""" +
                """\bConfiguration\.ORIENTATION_|\bLocalWindowInfo\s*\.\s*current\b|\bWindowInfoTracker\s*\.|""" +
                """\bdisplayMetrics\s*\.\s*(?:widthPixels|heightPixels)\b""",
        ),
        // A size or orientation property counts only when it is read from a Configuration
        accept = { match, facts ->
            val receiver = match.groups[1]?.value
            receiver == null || receiver.startsWith("LocalConfiguration") || receiver == "configuration" ||
                receiver in facts.configurationNames
        },
    ),
    UiKitRule(
        "A vertical list of rows is the kit's RowList (fastScroll = true for long lists); $ADAPTIVE_LAYOUT_SKILL",
        Regex("""\b(LazyColumn|FastScrollLazyColumn|ScrollbarLazyColumn)\s*[({]"""),
    ),
    UiKitRule(
        "Grid columns come from the kit: coverGridCells for covers, RowGridColumn for entries shown as rows, " +
            "CardFlow for cards; $ADAPTIVE_LAYOUT_SKILL",
        Regex("""\b(GridCells|StaggeredGridCells)\s*\.\s*(Fixed|Adaptive|FixedSize)\b"""),
    ),
)

/*
 * Readers and the player keep their own layouts until the user reviews each of them for the adaptive layout rules.
 * Every entry here is exempt only until that review of its reader or the player, and the review removes the entry.
 */
private val ADAPTIVE_LAYOUT_EXEMPT_PATHS = listOf(
    // Video player: exempt until its adaptive layout review, which removes this entry
    "entry-interactions/anime/src/main/java/eu/kanade/tachiyomi/ui/video/player/",
    // Chrome, settings and navigation the readers and the player share: exempt until the last of those reviews,
    // which removes these entries
    "entry-interactions/spi/src/main/java/mihon/entry/interactions/reader/",
    "entry-viewer-chrome-ui/src/main/java/mihon/entry/viewer/chrome/ui/",
    "entry-viewer-settings-ui/src/main/java/mihon/entry/viewer/settings/ui/",
)

private fun String.isAdaptiveLayoutExempt(): Boolean = ADAPTIVE_LAYOUT_EXEMPT_PATHS.any { startsWith(it) }

// Names a file gives the configuration, such as `val configuration = LocalConfiguration.current`
private val CONFIGURATION_ALIAS = Regex(
    """\bva[lr]\s+(\w+)\s*(?::\s*Configuration\s*)?=\s*""" +
        """(?:LocalConfiguration\s*\.\s*current|[\w.?]*\bconfiguration)\b""",
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
