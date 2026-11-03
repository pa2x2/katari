package mihon.entry.interactions.book.format.html.prosechapter.sanitization

/**
 * `@font-face` rules of a style sheet, reduced to what the document model carries: one font file per family.
 *
 * A family may be declared once per weight and style; the regular face wins and the reader synthesises bold
 * and italic from it. Within a rule, `src` lists alternatives in preference order; `local()` names and formats
 * Android can never read (EOT, SVG fonts) are left out.
 */
internal data class HtmlProseFontFace(
    val family: String,
    val sources: List<String>,
    val regular: Boolean,
)

internal object HtmlProseFontFaces {
    fun parse(styleSheet: String): List<HtmlProseFontFace> =
        FONT_FACE.findAll(styleSheet.replace(COMMENT, "")).mapNotNull { match ->
            val declarations = match.groupValues[1].topLevelDeclarations()
            val family = declarations["font-family"]?.normalizedFontFamily()?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val sources = declarations["src"]?.let(::readableSources).orEmpty()
            if (sources.isEmpty()) return@mapNotNull null
            HtmlProseFontFace(
                family = family,
                sources = sources,
                regular = declarations["font-weight"].isRegularValue("400") &&
                    declarations["font-style"].isRegularValue(),
            )
        }.toList()

    /**
     * Looks up a family's font resource: the first source of its preferred face that [resolveSource] turns into
     * a resource. Sources resolve only for families a document uses, since resolving registers them for loading
     * and download.
     */
    fun resolver(faces: List<HtmlProseFontFace>, resolveSource: (String) -> String?): (String) -> String? {
        val facesByFamily = faces.groupBy(HtmlProseFontFace::family)
        val resolved = mutableMapOf<String, String?>()
        return { family ->
            resolved.getOrPut(family) {
                facesByFamily[family].orEmpty().sortedByDescending(HtmlProseFontFace::regular)
                    .firstNotNullOfOrNull { face -> face.sources.firstNotNullOfOrNull(resolveSource) }
            }
        }
    }

    private fun readableSources(value: String): List<String> = SOURCE.findAll(value).mapNotNull { match ->
        val url = match.groupValues.subList(1, 4).firstOrNull(String::isNotEmpty)?.trim() ?: return@mapNotNull null
        val format = match.groupValues[4].trim().lowercase()
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        url.takeUnless {
            format in UNREADABLE_FORMATS || (format.isEmpty() && UNREADABLE_EXTENSIONS.any(path::endsWith))
        }
    }.toList()

    private fun String?.isRegularValue(vararg aliases: String): Boolean {
        val value = this?.trim()?.lowercase() ?: return true
        return value == "normal" || value in aliases
    }

    // A data: URI in src carries ';' and ',' of its own, so declarations split only outside quotes and parentheses.
    private fun String.topLevelDeclarations(): Map<String, String> {
        val declarations = mutableMapOf<String, String>()
        var depth = 0
        var quote: Char? = null
        var start = 0
        fun flush(end: Int) {
            val declaration = substring(start, end)
            val name = declaration.substringBefore(':', "").trim().lowercase()
            if (name.isNotEmpty()) declarations[name] = declaration.substringAfter(':').trim()
        }
        forEachIndexed { index, char ->
            when {
                quote != null -> if (char == quote) quote = null
                char == '"' || char == '\'' -> quote = char
                char == '(' -> depth++
                char == ')' -> depth = (depth - 1).coerceAtLeast(0)
                char == ';' && depth == 0 -> {
                    flush(index)
                    start = index + 1
                }
            }
        }
        flush(length)
        return declarations
    }

    private val COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
    private val FONT_FACE =
        Regex("@font-face\\s*\\{([^}]*)\\}", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val SOURCE = Regex(
        """url\(\s*(?:"([^"]*)"|'([^']*)'|([^)"']*))\s*\)\s*(?:format\(\s*["']?([^"')]*)["']?\s*\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val UNREADABLE_FORMATS = setOf("embedded-opentype", "svg")
    private val UNREADABLE_EXTENSIONS = listOf(".eot", ".svg", ".svgz")
}

/** The families of a `font-family` value in fallback order, normalised as `@font-face` families are. */
internal fun String.fontFamilyCandidates(): List<String> =
    split(',').map(String::normalizedFontFamily).filter(String::isNotEmpty)

internal fun String.normalizedFontFamily(): String = trim().trim('"', '\'').trim().lowercase()
