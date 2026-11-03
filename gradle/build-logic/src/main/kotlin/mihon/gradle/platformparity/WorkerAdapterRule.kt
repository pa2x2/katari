package mihon.gradle.platformparity

import mihon.gradle.tasks.withoutCommentsAndStrings

/*
 * A3. Android's WorkManager workers are the Android end of background work that desktop runs in-process, so a loop or
 * a helper in a worker is work desktop never gets. A worker only adapts a shared runner to WorkManager: its overrides,
 * its constructor and init block, the collaborators it holds as properties without accessors, and a companion of
 * `const` input keys. Its bodies hold no loop, neither a `while`, `for` or `do` nor a `forEach`, `map`, `mapNotNull`,
 * `filter`, `flatMap`, `onEach` or `collect`.
 */

internal fun workerAdapterFindings(sources: List<PlatformSource>): List<PlatformPlacementFinding> {
    return sources.filterNot { it.relativePath.isTestSource() }.flatMap(::workerFindings)
}

private fun workerFindings(source: PlatformSource): List<PlatformPlacementFinding> {
    val code = source.content.withoutCommentsAndStrings()
    return CLASS.findAll(code).flatMap { declaration ->
        val bodyStart = code.bodyStart(declaration.range.last + 1) ?: return@flatMap emptyList()
        val header = code.substring(declaration.range.last + 1, bodyStart)
        if (!WORKER_SUPERTYPE.containsMatchIn(header.withoutPrimaryConstructor())) return@flatMap emptyList()
        val worker = declaration.groupValues[1]
        val body = code.bracketed(bodyStart, '{', '}')
        memberFindings(code, body, worker) + loopFindings(code, body, worker)
    }
        .map { (offset, reason) -> PlatformPlacementFinding(source.relativePath, code.lineNumberAt(offset), reason) }
        .toList()
}

/** The declarations at the top of the worker's body that aren't what a worker may declare. */
private fun memberFindings(code: String, body: IntRange, worker: String): List<Pair<Int, String>> {
    return code.topLevelLines(body).mapNotNull { (offset, line) ->
        val declaration = line.withoutLeadingAnnotations()
        val keywords = declaration.withoutModifiers()
        when {
            declaration.startsWith("override ") -> null
            keywords.startsWith("init") && keywords.removePrefix("init").trimStart().startsWith("{") -> null
            keywords.startsWith("constructor") -> null
            keywords.startsWith("companion object") -> companionFinding(code, offset, worker)
            keywords.startsWith("get(") || keywords.startsWith("set(") ->
                offset to "$worker has a property accessor; workers hold collaborators, not logic"
            ACCESSOR.containsMatchIn(keywords) ->
                offset to "$worker has a property accessor; workers hold collaborators, not logic"
            keywords.startsWith("val ") || keywords.startsWith("var ") -> null
            keywords.startsWith("fun ") || keywords.startsWith("fun<") ->
                offset to "$worker declares the function ${keywords.functionName()}; move it into the shared runner " +
                    "the worker calls"
            NESTED_DECLARATION.containsMatchIn(keywords) ->
                offset to
                    "$worker declares a nested ${keywords.substringBefore(' ')}; move it next to the shared runner"
            else -> null
        }
    }
}

private fun companionFinding(code: String, offset: Int, worker: String): Pair<Int, String>? {
    val bodyStart = code.bodyStart(offset) ?: return null
    val nonConstant = code.topLevelLines(code.bracketed(bodyStart, '{', '}')).firstOrNull { (_, line) ->
        !line.withoutLeadingAnnotations().withoutModifiers(keepConst = true).startsWith("const val ")
    }
    return nonConstant?.let { (memberOffset, _) ->
        memberOffset to "$worker's companion holds more than const values; keep state and helpers in shared code"
    }
}

private fun loopFindings(code: String, body: IntRange, worker: String): List<Pair<Int, String>> {
    val text = code.substring(body)
    return (LOOP_KEYWORD.findAll(text) + LOOP_CALL.findAll(text)).map { match ->
        val loop = match.groupValues.drop(1).first(String::isNotEmpty)
        body.first + match.range.first to "$worker loops ($loop); move the loop into the shared runner the worker calls"
    }.toList()
}

/** The index of the `{` opening the body of the declaration whose header starts at [from], outside any brackets. */
private fun String.bodyStart(from: Int): Int? {
    var depth = 0
    for (index in from until length) {
        when (this[index]) {
            '(', '[' -> depth++
            ')', ']' -> depth--
            '{' -> if (depth == 0) return index
            '}' -> return null
        }
        if (depth == 0 && startsWith("\n\n", index)) return null
    }
    return null
}

/** Each line of [range] that starts outside every bracket opened within it, with its offset and its trimmed text. */
private fun String.topLevelLines(range: IntRange): List<Pair<Int, String>> {
    val lines = mutableListOf<Pair<Int, String>>()
    var depth = 0
    var lineStart = range.first
    for (index in range.first..range.last + 1) {
        if (index > range.last || this[index] == '\n') {
            val text = substring(lineStart, minOf(index, range.last + 1))
            val trimmed = text.trim()
            if (trimmed.isNotEmpty() && depth == 0) lines += lineStart + text.indexOf(trimmed) to trimmed
            depth +=
                text.count { it == '(' || it == '{' || it == '[' } - text.count { it == ')' || it == '}' || it == ']' }
            lineStart = index + 1
        }
    }
    return lines
}

private fun String.withoutPrimaryConstructor(): String {
    val open = indexOf('(')
    if (open < 0 || substring(0, open).contains(':')) return this
    return substring(bracketed(open, '(', ')').last + 2)
}

private fun String.withoutLeadingAnnotations(): String = replace(LEADING_ANNOTATION, "").trimStart()

private fun String.withoutModifiers(keepConst: Boolean = false): String {
    var rest = this
    while (true) {
        val modifier = MODIFIERS.firstOrNull { rest.startsWith("$it ") && !(keepConst && it == "const") } ?: return rest
        rest = rest.removePrefix(modifier).trimStart()
    }
}

private fun String.functionName(): String = FUNCTION_NAME.find(this)?.groupValues?.get(1) ?: "it declares"

/** The range strictly inside the [open] bracket at or after [from] and its matching [close]. */
private fun String.bracketed(from: Int, open: Char, close: Char): IntRange {
    val start = indexOf(open, from)
    if (start < 0) return IntRange.EMPTY
    var depth = 0
    for (index in start until length) {
        when (this[index]) {
            open -> depth++
            close -> if (--depth == 0) return start + 1 until index
        }
    }
    return start + 1 until length
}

private fun String.lineNumberAt(offset: Int): Int = 1 + (0 until offset).count { this[it] == '\n' }

private fun String.isTestSource(): Boolean =
    Regex("""/src/(?:test|androidTest|commonTest|desktopTest|androidHostTest|androidDeviceTest)\w*/""")
        .containsMatchIn(this)

private val CLASS = Regex("""\bclass\s+(\w+)""")

/** WorkManager's worker classes, as a supertype; qualified or not. */
private val WORKER_SUPERTYPE = Regex(""":\s*(?:[\w.]+\.)?(?:CoroutineWorker|Worker|ListenableWorker)\s*\(""")

private val LEADING_ANNOTATION = Regex("""^(?:@[\w.]+(?:\([^)]*\))?\s*)+""")

private val MODIFIERS = listOf(
    "private", "protected", "internal", "public", "lateinit", "suspend", "const", "open", "final", "inline",
    "operator", "infix", "tailrec", "data", "inner", "abstract", "sealed",
)

/** A property declared with an accessor on its own line: `val name: Type get() = …`. */
private val ACCESSOR = Regex("""^va[lr]\s+\w+(?:\s*:[^=]*?)?\s+(?:get|set)\s*\(""")

private val NESTED_DECLARATION = Regex("""^(?:class|object|interface|enum|typealias)\b""")

private val FUNCTION_NAME = Regex("""fun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)""")

private val LOOP_KEYWORD = Regex("""\b(while|for)\s*\(|\b(do)\s*\{""")

private val LOOP_CALL = Regex("""\b(forEach|map|mapNotNull|filter|flatMap|onEach|collect)\s*[({]""")
