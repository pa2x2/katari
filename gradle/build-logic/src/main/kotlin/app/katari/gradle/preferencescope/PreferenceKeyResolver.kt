package app.katari.gradle.preferencescope

/** A preference key as the source names it. */
internal sealed interface ResolvedKey {
    data class Exact(val key: String) : ResolvedKey

    /** Keys that start with [prefix] and end in a part only known at run time. */
    data class Family(val prefix: String) : ResolvedKey

    /** Under `Preference.appStateKey` or `Preference.privateKey`: the device's by definition, never marked. */
    data object DeviceByDefinition : ResolvedKey
}

/**
 * Reads the key a call passes from the source alone, the way keys are written in preference classes: string literals
 * and templates, `const val`s and `ProfilePreferenceKeyPattern.Prefix` families of the same file or named through
 * their class, `Preference.appStateKey`/`privateKey`, a `+` after a known prefix, a function of the same file that
 * returns any of these, and a parameter of the enclosing function, followed to every call of it in the same file.
 */
internal class PreferenceKeyResolver(files: List<KotlinSourceText>) {
    private val constants: Map<String, Map<String, ResolvedKey>> =
        files.associate { it.relativePath to constantsOf(it) }
    private val typeNames: Map<String, Set<String>> = files.associate { file ->
        file.relativePath to TYPE_DECLARATION.findAll(file.codeOutsideStrings).map { it.groupValues[1] }.toSet()
    }
    private val sharedConstants: Map<String, Set<ResolvedKey>> = buildMap<String, MutableSet<ResolvedKey>> {
        constants.values.forEach { fileConstants ->
            fileConstants.filterKeys { SCREAMING_CASE.matches(it) }.forEach { (name, key) ->
                getOrPut(name, ::mutableSetOf) += key
            }
        }
    }

    /** The keys [expression] in [file] at [offset] stands for, or null when the source doesn't tell. */
    fun resolve(file: KotlinSourceText, expression: String, offset: Int, depth: Int = 0): List<ResolvedKey>? {
        val value = expression.trim().removeNamedArgument("key")
        single(file, value, depth)?.let { return listOf(it) }
        val parameter = IDENTIFIER.matchEntire(value)?.value ?: return null
        return if (depth < MAX_DEPTH) callersOf(file, parameter, offset, depth) else null
    }

    private fun single(file: KotlinSourceText, value: String, depth: Int): ResolvedKey? {
        STRING_LITERAL.matchEntire(value)?.let { return template(file, it.groupValues[1]) }
        if (PREFIXED_KEY.containsMatchIn(value) && value.startsWith("Preference.")) {
            return ResolvedKey.DeviceByDefinition
        }
        FAMILY_KEY.matchEntire(value)?.let { match ->
            return constant(file, match.groupValues[1])?.takeIf { it !is ResolvedKey.Exact }
        }
        if (QUALIFIED_NAME.matches(value)) return constant(file, value)
        CONCATENATION.matchEntire(value)?.let { match ->
            val head = constant(file, match.groupValues[1]) as? ResolvedKey.Exact ?: return null
            return ResolvedKey.Family(head.key)
        }
        CALL.matchEntire(value)?.let { match ->
            if (depth >= MAX_DEPTH) return null
            val body = returnedExpression(file, match.groupValues[1]) ?: return null
            return single(file, body, depth + 1)
        }
        return null
    }

    private fun template(file: KotlinSourceText, content: String): ResolvedKey? {
        val prefix = StringBuilder()
        var index = 0
        while (index < content.length) {
            val char = content[index]
            when {
                char == '\\' && index + 1 < content.length -> {
                    prefix.append(content[index + 1])
                    index += 2
                }
                char == '$' -> {
                    val reference = TEMPLATE_REFERENCE.find(content, index)?.takeIf { it.range.first == index }
                    val name = reference?.let { it.groups[1]?.value ?: it.groups[2]?.value }
                    val part = name?.let { constant(file, it) as? ResolvedKey.Exact }
                        ?: return prefix.takeIf { it.isNotEmpty() }?.let { ResolvedKey.Family(it.toString()) }
                    prefix.append(part.key)
                    index = reference.range.last + 1
                }
                else -> {
                    prefix.append(char)
                    index++
                }
            }
        }
        return ResolvedKey.Exact(prefix.toString())
    }

    private fun constant(file: KotlinSourceText, name: String): ResolvedKey? {
        val simpleName = name.substringAfterLast('.')
        constants.getValue(file.relativePath)[name]?.let { return it }
        if ('.' in name) {
            val owner = name.substringBeforeLast('.').substringAfterLast('.')
            return typeNames.entries.firstNotNullOfOrNull { (path, types) ->
                constants.getValue(path)[simpleName]?.takeIf { owner in types }
            }
        }
        return sharedConstants[simpleName]?.singleOrNull()
    }

    /** The expression the function [name] of [file] returns, when its body is one. */
    private fun returnedExpression(file: KotlinSourceText, name: String): String? {
        val declaration = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?$name\s*\(""")
            .find(file.codeOutsideStrings) ?: return null
        val parameters = file.argumentsFrom(declaration.range.last + 1) ?: return null
        val body = BODY_START.find(file.code, parameters.end)
            ?.takeIf { it.range.first == parameters.end }
            ?: return null
        if (body.value.trimEnd().endsWith('=')) return expressionFrom(file, body.range.last + 1)
        // A block: the expression its last return gives
        val block = file.argumentsFrom(body.range.last + 1) ?: return null
        val lastReturn = RETURN.findAll(file.codeOutsideStrings.substring(0, block.end)).lastOrNull() ?: return null
        if (lastReturn.range.first < body.range.last) return null
        return expressionFrom(file, lastReturn.range.last + 1)
    }

    /** The expression starting at [start], up to the line break that ends it outside every bracket. */
    private fun expressionFrom(file: KotlinSourceText, start: Int): String {
        var depth = 0
        var index = start
        while (index < file.code.length) {
            when (file.codeOutsideStrings[index]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> if (depth-- == 0) break
                '\n' -> if (depth == 0 && !continuesOnNextLine(file, start, index)) break
            }
            index++
        }
        return file.code.substring(start, index).trim()
    }

    private fun continuesOnNextLine(file: KotlinSourceText, start: Int, lineEnd: Int): Boolean {
        val line = file.code.substring(start, lineEnd).trimEnd()
        val next = file.code.substring(lineEnd + 1).trimStart()
        return line.isEmpty() || line.endsWith('+') || line.endsWith('.') || next.startsWith('.')
    }

    private fun callersOf(file: KotlinSourceText, parameter: String, offset: Int, depth: Int): List<ResolvedKey>? {
        val function = FUNCTION.findAll(file.codeOutsideStrings.substring(0, offset)).lastOrNull() ?: return null
        val name = function.groupValues[1]
        val parameters = file.argumentsFrom(function.range.last + 1)?.values ?: return null
        val index = parameters.indexOfFirst { PARAMETER.find(it)?.groupValues?.get(1) == parameter }
        if (index < 0) return null
        val calls = Regex("""(?<![\w])$name\s*(?:<[^>()]*>)?\s*\(""").findAll(file.codeOutsideStrings)
            .filterNot { DECLARED.containsMatchIn(file.codeOutsideStrings.substring(0, it.range.first)) }
            .toList()
        if (calls.isEmpty()) return null
        return calls.flatMap { call ->
            val arguments = file.argumentsFrom(call.range.last + 1)?.values ?: return null
            val argument = arguments.firstOrNull { NAMED_ARGUMENT.find(it)?.groupValues?.get(1) == parameter }
                ?.substringAfter('=')
                ?: arguments.getOrNull(index)
                ?: return null
            resolve(file, argument, call.range.first, depth + 1) ?: return null
        }
    }

    private fun constantsOf(file: KotlinSourceText): Map<String, ResolvedKey> = buildMap {
        STRING_CONSTANT.findAll(file.code).forEach { put(it.groupValues[1], ResolvedKey.Exact(it.groupValues[2])) }
        FAMILY_CONSTANT.findAll(file.code).forEach { put(it.groupValues[1], ResolvedKey.Family(it.groupValues[2])) }
        PREFIXED_FAMILY_CONSTANT.findAll(file.code).forEach { put(it.groupValues[1], ResolvedKey.DeviceByDefinition) }
        FACTORY_FAMILY_CONSTANT.findAll(file.code).forEach { match ->
            val factory = returnedExpression(file, match.groupValues[2]) ?: return@forEach
            if (PREFIXED_KEY.containsMatchIn(factory)) put(match.groupValues[1], ResolvedKey.DeviceByDefinition)
        }
    }

    private fun String.removeNamedArgument(name: String): String =
        Regex("""^$name\s*=\s*""").find(this)?.let { substring(it.range.last + 1).trim() } ?: this

    private companion object {
        const val MAX_DEPTH = 3
        val IDENTIFIER = Regex("""[A-Za-z_]\w*""")
        val SCREAMING_CASE = Regex("""[A-Z][A-Z0-9_]*""")
        val QUALIFIED_NAME = Regex("""[A-Za-z_][\w.]*""")
        val STRING_LITERAL = Regex(""""((?:[^"\\]|\\.)*)"""", RegexOption.DOT_MATCHES_ALL)
        val TEMPLATE_REFERENCE = Regex("""\$(?:\{([\w.]+)}|([A-Za-z_]\w*))""")
        val PREFIXED_KEY = Regex("""\b(?:appStateKey|privateKey)\s*\(""")
        val FAMILY_KEY = Regex("""([\w.]+)\.key\s*\(.*\)""", RegexOption.DOT_MATCHES_ALL)
        val CONCATENATION = Regex("""([\w.]+)\s*\+.*""", RegexOption.DOT_MATCHES_ALL)
        val CALL = Regex("""(?:[\w.]+\.)?(\w+)\s*\(.*\)""", RegexOption.DOT_MATCHES_ALL)
        val BODY_START = Regex("""\s*(?::\s*[\w.<>?, ]+\s*)?(?:=|\{)""")
        val RETURN = Regex("""\breturn\s+""")
        val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\(""")
        val PARAMETER = Regex("""^\s*(?:vararg\s+|va[lr]\s+)?(\w+)\s*:""")
        val NAMED_ARGUMENT = Regex("""^\s*(\w+)\s*=(?!=)""")
        val DECLARED = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?$""")
        val TYPE_DECLARATION = Regex("""\b(?:class|object|interface)\s+(\w+)""")
        val STRING_CONSTANT = Regex("""\bval\s+(\w+)\s*(?::\s*String)?\s*=\s*"([^"\\$]*)"\s*$""", RegexOption.MULTILINE)
        val FAMILY_CONSTANT =
            Regex("""\bval\s+(\w+)\s*(?::[^=\n]+)?=\s*(?:ProfilePreferenceKeyPattern\.)?Prefix\(\s*"([^"\\$]*)"\s*\)""")
        val PREFIXED_FAMILY_CONSTANT = Regex(
            """\bval\s+(\w+)\s*(?::[^=\n]+)?=\s*(?:ProfilePreferenceKeyPattern\.)?Prefix\(\s*Preference\.""" +
                """(?:appStateKey|privateKey)\(""",
        )
        val FACTORY_FAMILY_CONSTANT = Regex("""\bval\s+(\w+)\s*(?::[^=\n]+)?=\s*(\w+)\(\s*"[^"\\$]*"\s*\)""")
    }
}
