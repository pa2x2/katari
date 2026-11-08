package app.katari.gradle.platformparity

/*
 * Reads the parts of Kotlin declarations the platform checks look at from code whose comments and string contents are
 * blanked: the classes, interfaces and objects (named, nested and anonymous) with their annotations, modifiers and
 * supertypes; the Metro bindings with the types and names they use; and the names and modifiers of top-level
 * declarations.
 * It reads code formatted by ktlint, as every source here is, and leaves out what it can't place rather than guess.
 */

internal enum class TypeKind { CLASS, INTERFACE, OBJECT }

internal data class TypeDeclaration(
    /** Null for an anonymous object. */
    val name: String?,
    val kind: TypeKind,
    val modifiers: Set<String>,
    /** The annotations' simple names. */
    val annotations: Set<String>,
    /** The supertypes as written, without type arguments or constructor calls. */
    val superTypes: List<String>,
    /** The type names in the primary constructor's parameters. */
    val constructorTypes: List<String>,
    /** Every identifier the body uses. */
    val bodyNames: Set<String>,
    val offset: Int,
    /** Null for anonymous objects and what they contain. */
    val qualifiedName: String?,
    val isTopLevel: Boolean,
)

internal data class BindingDeclaration(
    val annotations: Set<String>,
    val offset: Int,
    /** The type names in the receiver and parameter types. */
    val inputTypes: List<String>,
    /** The type names in the bound (return or property) type. */
    val boundTypes: List<String>,
    /** Every identifier the body uses. */
    val bodyNames: Set<String>,
    /** Where the body is in the file's code; empty for a binding without one. */
    val body: IntRange,
)

internal data class TopLevelDeclaration(val qualifiedName: String, val modifiers: Set<String>)

internal fun String.typeDeclarations(packageName: String): List<TypeDeclaration> {
    val depths = braceDepths()
    data class Found(val declaration: TypeDeclaration, val body: IntRange?)
    val found = mutableListOf<Found>()
    TYPE_KEYWORD.findAll(this).forEach { match ->
        val keywordStart = match.range.first
        if (keywordStart >= 2 && startsWith("::", keywordStart - 2)) return@forEach
        if (keywordStart > 0 && this[keywordStart - 1] == '.') return@forEach
        val header = headerBefore(keywordStart)
        val kind = when (match.value) {
            "interface" -> TypeKind.INTERFACE
            "object" -> TypeKind.OBJECT
            else -> TypeKind.CLASS
        }
        var index = skipSpaces(match.range.last + 1)
        val name = when {
            index < length && (this[index].isJavaIdentifierStart() || this[index] == '`') -> {
                val end = identifierEnd(index)
                substring(index, end).trim('`').also { index = end }
            }
            kind == TypeKind.OBJECT && "companion" in header.modifiers -> "Companion"
            kind == TypeKind.OBJECT -> null
            else -> return@forEach
        }
        index = skipBalanced(skipSpaces(index), '<', '>')
        index = skipConstructorModifiers(index)
        val parametersStart = skipSpaces(index)
        index = skipBalanced(parametersStart, '(', ')')
        val constructorTypes = if (index > parametersStart) {
            substring(parametersStart + 1, index - 1).topLevelSplit(',').flatMap { parameter ->
                parameter.substringAfter(':', "").substringBefore('=').typeNames()
            }
        } else {
            emptyList()
        }
        val afterParameters = skipWhitespace(index)
        val superTypes = if (afterParameters < length && this[afterParameters] == ':' &&
            !startsWith("::", afterParameters)
        ) {
            superTypesFrom(afterParameters + 1).also { (_, end) -> index = end }.first
        } else {
            emptyList()
        }
        var bodyStart = skipWhitespace(index)
        if (startsWith("where", bodyStart)) bodyStart = indexOf('{', bodyStart).takeIf { it >= 0 } ?: length
        val body = if (bodyStart < length && this[bodyStart] == '{') bracketedRange(bodyStart, '{', '}') else null
        found += Found(
            TypeDeclaration(
                name = name,
                kind = kind,
                modifiers = header.modifiers,
                annotations = header.annotations,
                superTypes = superTypes,
                constructorTypes = constructorTypes,
                bodyNames = body?.let { substring(it).identifiers() }.orEmpty(),
                offset = keywordStart,
                qualifiedName = null,
                isTopLevel = depths[keywordStart] == 0,
            ),
            body,
        )
    }
    val parents = found.map { (declaration, _) ->
        found.indices
            .filter { other -> found[other].body?.let { declaration.offset in it } == true }
            .maxByOrNull { found[it].declaration.offset }
    }
    val qualifiedNames = arrayOfNulls<String>(found.size)
    fun qualifiedName(index: Int): String? {
        qualifiedNames[index]?.let { return it }
        val declaration = found[index].declaration
        val name = declaration.name ?: return null
        val qualifiedName = when {
            declaration.isTopLevel -> if (packageName.isEmpty()) name else "$packageName.$name"
            else -> parents[index]?.let(::qualifiedName)?.let { "$it.$name" }
        }
        qualifiedNames[index] = qualifiedName
        return qualifiedName
    }
    return found.indices.map { found[it].declaration.copy(qualifiedName = qualifiedName(it)) }
}

internal fun String.bindingDeclarations(): List<BindingDeclaration> {
    return MEMBER_KEYWORD.findAll(this).mapNotNull { match ->
        val header = headerBefore(match.range.first)
        if (header.annotations.none { it in BINDING_ANNOTATIONS }) return@mapNotNull null
        val headerStart = header.start
        if (match.value ==
            "fun"
        ) {
            functionBinding(header, match.range.last + 1)
        } else {
            propertyBinding(header, match.range.last + 1)
                ?.copy(offset = headerStart)
        }
    }.toList()
}

internal fun String.topLevelDeclarations(packageName: String): List<TopLevelDeclaration> {
    val lines = split('\n')
    val chunks = mutableListOf<StringBuilder>()
    var current: StringBuilder? = null
    lines.forEach { line ->
        val startsDeclaration = line.isNotEmpty() && (line.first().isLetter() || line.first() == '@') &&
            !line.startsWith("package ") && !line.startsWith("import ") && !line.startsWith("@file:")
        val currentHasKeyword = current?.let { DECLARATION_KEYWORD.containsMatchIn(it) } == true
        if (startsDeclaration && (current == null || currentHasKeyword)) {
            current = StringBuilder().also { chunks += it }
        }
        if (line.startsWith("package ") || line.startsWith("import ") || line.startsWith("@file:")) current = null
        current?.appendLine(line)
    }
    return chunks.mapNotNull { text ->
        val code = text.toString()
        val keyword = DECLARATION_KEYWORD.find(code) ?: return@mapNotNull null
        val modifiers = code.substring(0, keyword.range.first).split(WHITESPACE)
            .filter { it in DECLARATION_MODIFIERS }
            .toSet()
        val name = code.declaredName(keyword.range.last + 1) ?: return@mapNotNull null
        TopLevelDeclaration(
            qualifiedName = if (packageName.isEmpty()) name else "$packageName.$name",
            modifiers = modifiers,
        )
    }
}

private data class Header(val start: Int, val modifiers: Set<String>, val annotations: Set<String>)

/** The annotations and modifiers right before [keywordStart], read backwards. */
private fun String.headerBefore(keywordStart: Int): Header {
    val modifiers = mutableSetOf<String>()
    val annotations = mutableSetOf<String>()
    var index = keywordStart
    var start = keywordStart
    while (true) {
        var cursor = index - 1
        while (cursor >= 0 && this[cursor].isWhitespace()) cursor--
        if (cursor < 0) break
        if (this[cursor] == ')') {
            val open = matchingOpenBefore(cursor, '(', ')') ?: break
            val wordEnd = open
            val wordStart = wordStartBefore(wordEnd)
            if (wordStart == wordEnd || wordStart == 0 || this[wordStart - 1] != '@') break
            annotations += substring(wordStart, wordEnd).substringAfterLast('.').substringAfterLast(':')
            index = wordStart - 1
            start = index
            continue
        }
        val wordStart = wordStartBefore(cursor + 1)
        if (wordStart > cursor) break
        val word = substring(wordStart, cursor + 1)
        when {
            wordStart > 0 && this[wordStart - 1] == '@' -> {
                annotations += word.substringAfterLast('.').substringAfterLast(':')
                index = wordStart - 1
            }
            word in DECLARATION_MODIFIERS -> {
                modifiers += word
                index = wordStart
            }
            else -> break
        }
        start = index
    }
    return Header(start, modifiers, annotations)
}

private fun String.functionBinding(header: Header, from: Int): BindingDeclaration? {
    var index = skipBalanced(skipSpaces(from), '<', '>')
    val open = indexOf('(', index).takeIf { it >= 0 } ?: return null
    val receiverAndName = substring(index, open).trim()
    val receiver = receiverAndName.substringBeforeLast('.', missingDelimiterValue = "")
    val parameters = bracketedRange(open, '(', ')')
    index = parameters.last + 2
    val parameterTypes = substring(parameters).topLevelSplit(',').map {
        it.substringAfter(':', "").substringBefore('=')
    }
    var cursor = skipSpaces(index)
    val returnType = if (cursor < length && this[cursor] == ':') {
        val end = typeEnd(cursor + 1)
        substring(cursor + 1, end).also { cursor = end }
    } else {
        ""
    }
    cursor = skipSpaces(cursor)
    val body = bodyAt(cursor, header.start)
    return BindingDeclaration(
        annotations = header.annotations,
        offset = header.start,
        inputTypes = (listOf(receiver) + parameterTypes).flatMap { it.typeNames() },
        boundTypes = returnType.typeNames(),
        bodyNames = substring(body).identifiers(),
        body = body,
    )
}

private fun String.propertyBinding(header: Header, from: Int): BindingDeclaration? {
    val index = skipBalanced(skipSpaces(from), '<', '>')
    val colon = (index until length).firstOrNull { this[it] == ':' || this[it] == '\n' || this[it] == '=' }
        ?.takeIf { this[it] == ':' }
        ?: return null
    val receiverAndName = substring(index, colon).trim()
    val receiver = receiverAndName.substringBeforeLast('.', missingDelimiterValue = "")
    val typeEnd = typeEnd(colon + 1)
    var cursor = skipWhitespace(typeEnd)
    if (startsWith("get()", cursor)) cursor = skipSpaces(cursor + "get()".length)
    val body = bodyAt(cursor, header.start)
    return BindingDeclaration(
        annotations = header.annotations,
        offset = header.start,
        inputTypes = receiver.typeNames(),
        boundTypes = substring(colon + 1, typeEnd).typeNames(),
        bodyNames = substring(body).identifiers(),
        body = body,
    )
}

/** The body starting at [cursor]: a block, or an expression running until the next member at the binding's indent. */
private fun String.bodyAt(cursor: Int, headerStart: Int): IntRange {
    if (cursor >= length) return IntRange.EMPTY
    if (this[cursor] == '{') return bracketedRange(cursor, '{', '}')
    if (this[cursor] != '=') return IntRange.EMPTY
    val indent = headerStart - (lastIndexOf('\n', headerStart - 1) + 1)
    var lineStart = indexOf('\n', cursor).let { if (it < 0) return cursor + 1 until length else it + 1 }
    while (lineStart < length) {
        val lineEnd = indexOf('\n', lineStart).let { if (it < 0) length else it }
        val line = substring(lineStart, lineEnd)
        val lineIndent = line.indexOfFirst { !it.isWhitespace() }
        if (lineIndent >= 0) {
            if (lineIndent < indent) break
            if (lineIndent == indent && line[lineIndent] !in ")]}.?:") break
        }
        lineStart = lineEnd + 1
    }
    return cursor + 1 until lineStart.coerceAtMost(length)
}

/** Where a type written from [from] ends: at `=`, `{`, `by` or the line's end outside brackets. */
private fun String.typeEnd(from: Int): Int {
    var depth = 0
    var index = from
    while (index < length) {
        val char = this[index]
        when {
            char == '<' || char == '(' -> depth++
            char == '>' && this[index - 1] != '-' -> depth--
            char == ')' -> depth--
            depth == 0 && (char == '=' || char == '{' || char == '\n') -> return index
            depth == 0 && startsWith(" by ", index) -> return index
        }
        index++
    }
    return length
}

private fun String.superTypesFrom(from: Int): Pair<List<String>, Int> {
    val superTypes = mutableListOf<String>()
    var index = from
    while (true) {
        index = skipWhitespace(index)
        while (index < length && this[index] == '@') {
            index = identifierEnd(index + 1, allowDots = true)
            if (index < length && this[index] == '(') index = skipBalanced(index, '(', ')')
            index = skipWhitespace(index)
        }
        val nameEnd = identifierEnd(index, allowDots = true)
        if (nameEnd == index) break
        superTypes += substring(index, nameEnd)
        index = skipBalanced(nameEnd, '<', '>')
        index = skipBalanced(skipSpaces(index), '(', ')')
        index = skipSpaces(index)
        if (startsWith("by", index) && index + 2 < length && !this[index + 2].isJavaIdentifierPart()) {
            index = delegateEnd(index + 2)
        }
        index = skipSpaces(index)
        if (index < length && this[index] == ',') {
            index++
            continue
        }
        break
    }
    return superTypes to index
}

private fun String.delegateEnd(from: Int): Int {
    var depth = 0
    var index = from
    while (index < length) {
        when (this[index]) {
            '(', '[', '<' -> depth++
            ')', ']', '>' -> depth--
            ',', '{', '\n' -> if (depth == 0) return index
        }
        index++
    }
    return length
}

private fun String.skipConstructorModifiers(from: Int): Int {
    var index = skipSpaces(from)
    while (index < length) {
        when {
            this[index] == '@' -> {
                index = identifierEnd(index + 1, allowDots = true)
                if (index < length && this[index] == '(') index = skipBalanced(index, '(', ')')
            }
            CONSTRUCTOR_MODIFIER.matchesAt(this, index) -> index = identifierEnd(index)
            else -> return index
        }
        index = skipSpaces(index)
    }
    return index
}

private fun String.declaredName(from: Int): String? {
    var index = skipBalanced(skipSpaces(from), '<', '>')
    index = skipSpaces(index)
    val end = (index until length).firstOrNull { this[it] in "(:=\n{" } ?: length
    val receiverAndName = substring(index, end).trim()
    var depth = 0
    var lastDot = -1
    receiverAndName.forEachIndexed { position, char ->
        when (char) {
            '<' -> depth++
            '>' -> depth--
            '.' -> if (depth == 0) lastDot = position
        }
    }
    return receiverAndName.substring(lastDot + 1).trim('`', ' ').takeIf { name ->
        name.isNotEmpty() && name.all { it.isJavaIdentifierPart() }
    }
}

/** Every simple type name in a type as written: `Map<EntryType, Foo.Bar>` gives `Map`, `EntryType` and `Foo.Bar`. */
internal fun String.typeNames(): List<String> = TYPE_NAME.findAll(this).map { it.value }.toList()

/** The identifiers in this code, and the dotted chains they form, such as `PlatformContext.INSTANCE`. */
private fun String.identifiers(): Set<String> =
    (IDENTIFIER.findAll(this) + NAME_CHAIN.findAll(this)).map { it.value }.toSet()

private fun String.topLevelSplit(separator: Char): List<String> {
    val parts = mutableListOf<String>()
    var depth = 0
    var start = 0
    forEachIndexed { index, char ->
        when {
            char == '(' || char == '<' || char == '[' || char == '{' -> depth++
            char == ')' || char == ']' || char == '}' -> depth--
            char == '>' && (index == 0 || this[index - 1] != '-') -> depth--
            char == separator && depth == 0 -> {
                parts += substring(start, index)
                start = index + 1
            }
        }
    }
    parts += substring(start)
    return parts.filter { it.isNotBlank() }
}

internal fun String.braceDepths(): IntArray {
    val depths = IntArray(length + 1)
    var depth = 0
    for (index in indices) {
        depths[index] = depth
        when (this[index]) {
            '{' -> depth++
            '}' -> depth = (depth - 1).coerceAtLeast(0)
        }
    }
    depths[length] = depth
    return depths
}

private fun String.skipSpaces(from: Int): Int {
    var index = from
    while (index < length && (this[index] == ' ' || this[index] == '\t')) index++
    return index
}

private fun String.skipWhitespace(from: Int): Int {
    var index = from
    while (index < length && this[index].isWhitespace()) index++
    return index
}

private fun String.identifierEnd(from: Int, allowDots: Boolean = false): Int {
    if (from < length && this[from] == '`') return (indexOf('`', from + 1).takeIf { it >= 0 } ?: length - 1) + 1
    var index = from
    while (index < length &&
        (this[index].isJavaIdentifierPart() || (allowDots && this[index] == '.' && index > from))
    ) {
        index++
    }
    return index
}

private fun String.wordStartBefore(end: Int): Int {
    var index = end
    while (index > 0 && (this[index - 1].isJavaIdentifierPart() || this[index - 1] == '.' || this[index - 1] == ':')) {
        index--
    }
    return index
}

/** The index after the bracket [open] that starts at [from] and its match, or [from] when none starts there. */
private fun String.skipBalanced(from: Int, open: Char, close: Char): Int {
    if (from >= length || this[from] != open) return from
    val range = bracketedRange(from, open, close)
    return (range.last + 2).coerceAtMost(length)
}

/** The range strictly inside the [open] bracket at [from] and its matching [close]. */
internal fun String.bracketedRange(from: Int, open: Char, close: Char): IntRange {
    var depth = 0
    for (index in from until length) {
        val char = this[index]
        when {
            char == open -> depth++
            char == close && !(close == '>' && index > 0 && this[index - 1] == '-') -> if (--depth == 0) {
                return from + 1 until index
            }
        }
    }
    return from + 1 until length
}

private fun String.matchingOpenBefore(closeIndex: Int, open: Char, close: Char): Int? {
    var depth = 0
    for (index in closeIndex downTo 0) {
        when (this[index]) {
            close -> depth++
            open -> if (--depth == 0) return index
        }
    }
    return null
}

private val TYPE_KEYWORD = Regex("""\b(?:class|interface|object)\b""")

private val MEMBER_KEYWORD = Regex("""\b(?:fun|val|var)\b""")

private val DECLARATION_KEYWORD = Regex("""\b(?:fun|val|var|class|interface|object|typealias)\b""")

private val CONSTRUCTOR_MODIFIER = Regex("""(?:private|internal|protected|public|constructor)\b""")

private val TYPE_NAME = Regex("""(?<![\w.])[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)*""")

private val IDENTIFIER = Regex("""[A-Za-z_]\w*""")

private val NAME_CHAIN = Regex("""(?<![\w.])[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+""")

private val BINDING_ANNOTATIONS = setOf("Binds", "Provides", "Multibinds")

private val DECLARATION_MODIFIERS = setOf(
    "public", "private", "internal", "protected", "abstract", "open", "final", "sealed", "data", "enum", "annotation",
    "value", "inline", "inner", "companion", "fun", "expect", "actual", "override", "lateinit", "const", "suspend",
    "operator", "infix", "tailrec", "external",
)

private val WHITESPACE = Regex("""\s+""")
