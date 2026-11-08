package app.katari.gradle.kotlinsource

import java.io.File

/**
 * Rules name files by their Android-library path (`core/domain/src/main/java/…`), so they hold for a module before and
 * after it becomes multiplatform: files in `commonMain`, `androidMain` or `desktopMain` are indexed under that path,
 * and findings are reported at the real file. A platform file that shares its name with a file of another source set,
 * as an `actual` beside its `expect` may, keeps its source set in its indexed name (`androidMain/…/Foo.kt` →
 * `main/java/…/Foo.android.kt`).
 */
internal class KotlinSourceIndex private constructor(
    val files: List<KotlinSourceFile>,
    private val filePaths: Map<String, String>,
) {
    fun filePath(relativePath: String): String = filePaths[relativePath] ?: relativePath

    companion object {
        fun create(root: File, sourceFiles: List<File>): KotlinSourceIndex {
            val filePaths = mutableMapOf<String, String>()
            val sourcePaths = sourceFiles.associateWith { it.relativeTo(root).invariantSeparatorsPath }
            val sharedRulePaths = sourcePaths.values.groupingBy { it.toRulePath() }.eachCount().filterValues { it > 1 }
            val indexedFiles = sourceFiles.map { file ->
                val filePath = sourcePaths.getValue(file)
                val rulePath = filePath.toRulePath()
                val relativePath = if (rulePath in sharedRulePaths) filePath.toPlatformRulePath() else rulePath
                check(filePaths.put(relativePath, filePath) == null) { "$filePath duplicates $relativePath" }
                KotlinSourceFile.from(relativePath = relativePath, content = file.readText())
            }
            return KotlinSourceIndex(indexedFiles, filePaths)
        }
    }
}

private val MULTIPLATFORM_SOURCE_DIRECTORY = Regex("""/src/(?:main|commonMain|(android|desktop)Main)/kotlin/""")

private fun String.toRulePath(): String {
    val sourceDirectory = MULTIPLATFORM_SOURCE_DIRECTORY.find(this) ?: return this
    return replaceRange(sourceDirectory.range, "/src/main/java/")
}

private fun String.toPlatformRulePath(): String {
    val path = toRulePath()
    val platform = MULTIPLATFORM_SOURCE_DIRECTORY.find(this)?.groupValues?.get(1).orEmpty().ifEmpty { return path }
    return if (path.endsWith(".$platform.kt")) path else path.removeSuffix(".kt") + ".$platform.kt"
}

internal data class KotlinSourceFile(
    val relativePath: String,
    val content: String,
    val packageName: String,
    val imports: List<KotlinImport>,
    val topLevelDeclarations: List<KotlinDeclaration>,
    val references: List<KotlinReference>,
) {
    private val lineStarts: List<Int> = content.lineStarts()

    fun lineNumberOf(pattern: String): Int? {
        return content.lines()
            .indexOfFirst { it.contains(pattern) }
            .takeIf { it >= 0 }
            ?.plus(1)
    }

    fun publicTopLevelPropertyNames(): Sequence<String> {
        return content.lineSequence().mapNotNull { line ->
            PUBLIC_TOP_LEVEL_PROPERTY.matchEntire(line)?.groupValues?.get(1)
        }
    }

    companion object {
        private val PUBLIC_TOP_LEVEL_PROPERTY =
            Regex("""(?:(?:public|const|lateinit)\s+)*(?:val|var)\s+([A-Za-z_][A-Za-z0-9_]*).*$""")

        fun from(relativePath: String, content: String): KotlinSourceFile {
            val lineStarts = content.lineStarts()
            val tokens = content.kotlinTokens(lineStarts)
            val packageName = tokens.packageName(content)
            return KotlinSourceFile(
                relativePath = relativePath,
                content = content,
                packageName = packageName,
                imports = content.imports(),
                topLevelDeclarations = tokens.topLevelDeclarations(packageName),
                references = tokens
                    .filter { it.type == KotlinTokenType.IDENTIFIER }
                    .map { KotlinReference(it.text, it.lineNumber) },
            )
        }
    }
}

internal data class KotlinImport(
    val importedFqName: String?,
    val lineNumber: Int,
    val alias: String? = null,
    /** `import a.b.*`, whose [importedFqName] is the package `a.b`. */
    val isStar: Boolean = false,
)

internal data class KotlinDeclaration(
    val name: String,
    val qualifiedName: String,
    val kind: KotlinDeclarationKind,
    val isPublic: Boolean,
    val superTypeNames: Set<String>,
    val returnTypeName: String?,
    val lineNumber: Int,
)

internal enum class KotlinDeclarationKind(
    val label: String,
) {
    CLASS("class"),
    INTERFACE("interface"),
    OBJECT("object"),
    FUNCTION("function"),
    TYPE_ALIAS("type alias"),
}

internal data class KotlinReference(
    val name: String,
    val lineNumber: Int,
)

private data class KotlinToken(
    val type: KotlinTokenType,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val lineNumber: Int,
)

private enum class KotlinTokenType {
    IDENTIFIER,
    PACKAGE_KEYWORD,
    CLASS_KEYWORD,
    INTERFACE_KEYWORD,
    OBJECT_KEYWORD,
    FUN_KEYWORD,
    TYPE_ALIAS_KEYWORD,
    WHERE_KEYWORD,
    LBRACE,
    RBRACE,
    LPAR,
    RPAR,
    COLON,
    COLONCOLON,
    EQ,
    EOL_OR_SEMICOLON,
    WHITE_SPACE,
    OTHER,
}

private fun String.kotlinTokens(lineStarts: List<Int>): List<KotlinToken> {
    val tokens = mutableListOf<KotlinToken>()
    var offset = 0

    fun addToken(
        type: KotlinTokenType,
        startOffset: Int,
        endOffset: Int,
        text: String = substring(
            startOffset,
            endOffset,
        ),
    ) {
        tokens += KotlinToken(
            type = type,
            text = text,
            startOffset = startOffset,
            endOffset = endOffset,
            lineNumber = lineStarts.lineNumberFor(startOffset),
        )
    }

    while (offset < length) {
        val startOffset = offset
        when {
            this[offset] == ' ' || this[offset] == '\t' || this[offset] == '\r' -> {
                offset += 1
                while (offset < length && (this[offset] == ' ' || this[offset] == '\t' || this[offset] == '\r')) {
                    offset += 1
                }
                addToken(KotlinTokenType.WHITE_SPACE, startOffset, offset)
            }
            this[offset] == '\n' || this[offset] == ';' -> {
                offset += 1
                addToken(KotlinTokenType.EOL_OR_SEMICOLON, startOffset, offset)
            }
            startsWith("//", offset) -> {
                offset = indexOf('\n', offset).takeIf { it >= 0 } ?: length
            }
            startsWith("/*", offset) -> {
                offset = commentEndOffset(offset)
            }
            startsWith("\"\"\"", offset) -> {
                offset = rawStringEndOffset(offset)
            }
            this[offset] == '"' || this[offset] == '\'' -> {
                offset = quotedLiteralEndOffset(offset, this[offset])
            }
            this[offset] == '`' -> {
                offset = indexOf('`', offset + 1).takeIf { it >= 0 }?.plus(1) ?: length
                addToken(
                    KotlinTokenType.IDENTIFIER,
                    startOffset,
                    offset,
                    substring(startOffset + 1, (offset - 1).coerceAtLeast(startOffset + 1)),
                )
            }
            this[offset].isJavaIdentifierStart() -> {
                offset += 1
                while (offset < length && this[offset].isJavaIdentifierPart()) offset += 1
                val text = substring(startOffset, offset)
                addToken(text.kotlinTokenType(), startOffset, offset)
            }
            startsWith("::", offset) -> {
                offset += 2
                addToken(KotlinTokenType.COLONCOLON, startOffset, offset)
            }
            else -> {
                offset += 1
                val type = when (this[startOffset]) {
                    '{' -> KotlinTokenType.LBRACE
                    '}' -> KotlinTokenType.RBRACE
                    '(' -> KotlinTokenType.LPAR
                    ')' -> KotlinTokenType.RPAR
                    ':' -> KotlinTokenType.COLON
                    '=' -> KotlinTokenType.EQ
                    else -> KotlinTokenType.OTHER
                }
                addToken(type, startOffset, offset)
            }
        }
    }
    return tokens
}

private fun String.commentEndOffset(startOffset: Int): Int {
    var offset = startOffset + 2
    var depth = 1
    while (offset < length && depth > 0) {
        when {
            startsWith("/*", offset) -> {
                depth += 1
                offset += 2
            }
            startsWith("*/", offset) -> {
                depth -= 1
                offset += 2
            }
            else -> offset += 1
        }
    }
    return offset
}

private fun String.rawStringEndOffset(startOffset: Int): Int {
    val closingOffset = indexOf("\"\"\"", startOffset + 3)
    return if (closingOffset >= 0) closingOffset + 3 else length
}

private fun String.quotedLiteralEndOffset(startOffset: Int, quote: Char): Int {
    var offset = startOffset + 1
    while (offset < length) {
        when {
            this[offset] == '\\' -> offset = (offset + 2).coerceAtMost(length)
            this[offset] == quote -> return offset + 1
            else -> offset += 1
        }
    }
    return offset
}

private fun String.kotlinTokenType(): KotlinTokenType {
    return when (this) {
        "package" -> KotlinTokenType.PACKAGE_KEYWORD
        "class" -> KotlinTokenType.CLASS_KEYWORD
        "interface" -> KotlinTokenType.INTERFACE_KEYWORD
        "object" -> KotlinTokenType.OBJECT_KEYWORD
        "fun" -> KotlinTokenType.FUN_KEYWORD
        "typealias" -> KotlinTokenType.TYPE_ALIAS_KEYWORD
        "where" -> KotlinTokenType.WHERE_KEYWORD
        else -> KotlinTokenType.IDENTIFIER
    }
}

private fun List<KotlinToken>.packageName(content: String): String {
    val packageToken = firstOrNull { it.type == KotlinTokenType.PACKAGE_KEYWORD } ?: return ""
    return content.lineTail(packageToken.endOffset)
        .substringBefore("//")
        .trim()
        .removeSuffix(";")
        .trim()
}

private fun String.imports(): List<KotlinImport> {
    return lines()
        .mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null

            val directive = trimmed
                .removePrefix("import ")
                .substringBefore("//")
                .trim()
                .removeSuffix(";")
            val importedName = directive.substringBefore(" as ").trim()
            val importedFqName = importedName
                .removeSuffix(".*")
                .trim()
                .takeIf { it.isNotEmpty() }

            KotlinImport(
                importedFqName = importedFqName,
                lineNumber = index + 1,
                alias = directive.substringAfter(" as ", missingDelimiterValue = "").trim().ifEmpty { null },
                isStar = importedName.endsWith(".*"),
            )
        }
}

private fun List<KotlinToken>.topLevelDeclarations(packageName: String): List<KotlinDeclaration> {
    val declarations = mutableListOf<KotlinDeclaration>()
    var braceDepth = 0
    val modifiers = mutableSetOf<String>()

    forEachIndexed { index, token ->
        if (braceDepth == 0) {
            if (token.text in TOP_LEVEL_MODIFIER_TEXTS) {
                modifiers += token.text
            }

            val kind = token.type.declarationKind()
            if (kind != null) {
                if (!isClassLiteralToken(index)) {
                    declarationAt(index, kind, packageName, modifiers)?.let { declarations += it }
                }
                modifiers.clear()
            }
        }

        when (token.type) {
            KotlinTokenType.LBRACE -> braceDepth += 1
            KotlinTokenType.RBRACE -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
            else -> Unit
        }
    }

    return declarations
}

private fun List<KotlinToken>.isClassLiteralToken(index: Int): Boolean {
    if (this[index].type != KotlinTokenType.CLASS_KEYWORD) return false
    return take(index)
        .lastOrNull { it.type != KotlinTokenType.WHITE_SPACE && it.type != KotlinTokenType.EOL_OR_SEMICOLON }
        ?.type == KotlinTokenType.COLONCOLON
}

private fun List<KotlinToken>.declarationAt(
    keywordIndex: Int,
    kind: KotlinDeclarationKind,
    packageName: String,
    modifiers: Set<String>,
): KotlinDeclaration? {
    val keyword = this[keywordIndex]
    val nameToken = drop(keywordIndex + 1)
        .firstOrNull { it.type == KotlinTokenType.IDENTIFIER }
        ?: return null
    val name = nameToken.text

    return KotlinDeclaration(
        name = name,
        qualifiedName = "$packageName.$name",
        kind = kind,
        isPublic = modifiers.none { it == "private" || it == "internal" || it == "protected" },
        superTypeNames = if (kind == KotlinDeclarationKind.CLASS ||
            kind == KotlinDeclarationKind.INTERFACE ||
            kind == KotlinDeclarationKind.OBJECT
        ) {
            superTypeNamesAfter(keywordIndex)
        } else {
            emptySet()
        },
        returnTypeName = if (kind == KotlinDeclarationKind.FUNCTION) {
            returnTypeNameAfter(nameToken)
        } else {
            null
        },
        lineNumber = keyword.lineNumber,
    )
}

private fun List<KotlinToken>.superTypeNamesAfter(keywordIndex: Int): Set<String> {
    var parenDepth = 0
    var inSuperTypes = false
    val superTypeNames = mutableSetOf<String>()

    drop(keywordIndex + 1).forEach { token ->
        when (token.type) {
            KotlinTokenType.LPAR -> parenDepth += 1
            KotlinTokenType.RPAR -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
            KotlinTokenType.COLON -> if (parenDepth == 0) inSuperTypes = true
            KotlinTokenType.LBRACE, KotlinTokenType.EQ, KotlinTokenType.WHERE_KEYWORD -> return superTypeNames
            KotlinTokenType.IDENTIFIER -> if (inSuperTypes) superTypeNames += token.text.toSimpleTypeName()
            else -> Unit
        }
    }

    return superTypeNames
}

private fun List<KotlinToken>.returnTypeNameAfter(nameToken: KotlinToken): String? {
    var parenDepth = 0
    var collectReturnType = false
    val returnType = StringBuilder()

    dropWhile { it !== nameToken }
        .drop(1)
        .forEach { token ->
            when (token.type) {
                KotlinTokenType.LPAR -> parenDepth += 1
                KotlinTokenType.RPAR -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                KotlinTokenType.COLON -> if (parenDepth == 0) collectReturnType = true
                KotlinTokenType.LBRACE, KotlinTokenType.EQ, KotlinTokenType.EOL_OR_SEMICOLON -> {
                    if (collectReturnType) return returnType.toString().toSimpleTypeName()
                }
                else -> if (collectReturnType) returnType.append(token.text)
            }
        }

    return returnType.toString()
        .takeIf { it.isNotBlank() }
        ?.toSimpleTypeName()
}

private fun KotlinTokenType.declarationKind(): KotlinDeclarationKind? {
    return when (this) {
        KotlinTokenType.CLASS_KEYWORD -> KotlinDeclarationKind.CLASS
        KotlinTokenType.INTERFACE_KEYWORD -> KotlinDeclarationKind.INTERFACE
        KotlinTokenType.OBJECT_KEYWORD -> KotlinDeclarationKind.OBJECT
        KotlinTokenType.FUN_KEYWORD -> KotlinDeclarationKind.FUNCTION
        KotlinTokenType.TYPE_ALIAS_KEYWORD -> KotlinDeclarationKind.TYPE_ALIAS
        else -> null
    }
}

private fun String.lineTail(startOffset: Int): String {
    val endOffset = indexOf('\n', startOffset).takeIf { it >= 0 } ?: length
    return substring(startOffset, endOffset)
}

private fun String.toSimpleTypeName(): String {
    return substringBefore("<")
        .substringBefore("(")
        .substringAfterLast(".")
        .trim()
}

private fun String.lineStarts(): List<Int> {
    val starts = mutableListOf(0)
    forEachIndexed { index, char ->
        if (char == '\n' && index + 1 < length) {
            starts += index + 1
        }
    }
    return starts
}

private val TOP_LEVEL_MODIFIER_TEXTS = setOf(
    "public",
    "private",
    "internal",
    "protected",
    "data",
    "sealed",
    "abstract",
    "open",
    "value",
)

private fun List<Int>.lineNumberFor(offset: Int): Int {
    val index = binarySearch(offset)
    return if (index >= 0) {
        index + 1
    } else {
        -index - 1
    }
}
