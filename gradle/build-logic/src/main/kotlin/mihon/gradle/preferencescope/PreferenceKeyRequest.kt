package mihon.gradle.preferencescope

/** A key a preference store is asked for, or a key declared to be one, at a place in the source. */
internal class PreferenceKeyRequest(
    val relativePath: String,
    val lineNumber: Int,
    val expression: String,
    /** Null when the source doesn't tell which key it is. */
    val keys: List<ResolvedKey>?,
)

/**
 * Every key the sources ask a preference store for: the first argument, or `key =`, of every store getter called in a
 * file that works with preferences, and the keys handed to classes that carry a key for a store to read later.
 */
internal fun preferenceKeyRequests(files: List<KotlinSourceText>): List<PreferenceKeyRequest> {
    val resolver = PreferenceKeyResolver(files)
    return files.flatMap { file ->
        val getterCalls = if (PREFERENCE_CODE.containsMatchIn(file.code)) getterCalls(file, resolver) else emptyList()
        getterCalls + carriedKeys(file, resolver)
    }
}

private fun getterCalls(file: KotlinSourceText, resolver: PreferenceKeyResolver): List<PreferenceKeyRequest> {
    return GETTER_CALL.findAll(file.codeOutsideStrings).mapNotNull { call ->
        val before = file.codeOutsideStrings.substring(0, call.range.first)
        if (DECLARATION_BEFORE.containsMatchIn(before)) return@mapNotNull null
        // A getter of its own, such as getEnum, passes on its caller's key; its callers are requests themselves.
        val enclosing = ENCLOSING_FUNCTION.findAll(before).lastOrNull()?.groupValues?.get(1)
        if (enclosing in GETTERS) return@mapNotNull null
        val arguments = file.argumentsFrom(call.range.last + 1)?.values ?: return@mapNotNull null
        val key = arguments.firstOrNull { it.startsWith("key") && NAMED_KEY.containsMatchIn(it) }
            ?: arguments.firstOrNull()
            ?: return@mapNotNull null
        // Resource and column lookups share the getters' names
        if (NOT_A_KEY.containsMatchIn(key)) return@mapNotNull null
        request(file, resolver, call.range.first, key)
    }.toList()
}

private fun carriedKeys(file: KotlinSourceText, resolver: PreferenceKeyResolver): List<PreferenceKeyRequest> {
    return KEY_CARRIERS.flatMap { (carrier, keyParameters) ->
        Regex("""(?<![\w.])$carrier\s*\(""").findAll(file.codeOutsideStrings)
            .filterNot { DECLARATION_BEFORE.containsMatchIn(file.codeOutsideStrings.substring(0, it.range.first)) }
            .filterNot { TYPE_BEFORE.containsMatchIn(file.codeOutsideStrings.substring(0, it.range.first)) }
            .flatMap { call ->
                val arguments = file.argumentsFrom(call.range.last + 1)?.values.orEmpty()
                keyParameters.mapIndexedNotNull { index, parameter ->
                    val named = arguments.firstOrNull { Regex("""^$parameter\s*=(?!=)""").containsMatchIn(it) }
                    val argument = named?.substringAfter('=')
                        ?: arguments.getOrNull(index)?.takeIf { index == 0 && !NAMED.containsMatchIn(it) }
                        ?: return@mapIndexedNotNull null
                    request(file, resolver, call.range.first, argument)
                }
            }
            .toList()
    }
}

private fun request(file: KotlinSourceText, resolver: PreferenceKeyResolver, offset: Int, expression: String) =
    PreferenceKeyRequest(
        relativePath = file.relativePath,
        lineNumber = file.lineOf(offset),
        expression = expression.trim(),
        keys = resolver.resolve(file, expression, offset),
    )

private val GETTERS = setOf(
    "getString",
    "getLong",
    "getInt",
    "getFloat",
    "getBoolean",
    "getStringSet",
    "getObjectFromString",
    "getObjectSetFromStringSet",
    "getEnum",
    "getEnumSet",
    "getLongArray",
)

/** Classes whose constructor takes a key that a store is later asked for, with the parameters that hold one. */
private val KEY_CARRIERS = mapOf(
    "EntryMediaCacheAutoClearPreference" to listOf("key", "seedFromKeyWhenAbsent"),
)

private val PREFERENCE_CODE = Regex("""tachiyomi\.core\.common\.preference|\bPreferenceStore\b|\bProfileStore\b""")
private val GETTER_CALL = Regex("""(?<![\w])(${GETTERS.joinToString("|")})\s*(?:<[^>()]*>)?\s*\(""")
private val DECLARATION_BEFORE = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?$""")
private val TYPE_BEFORE = Regex("""\b(?:class|interface|object)\s+$""")
private val ENCLOSING_FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\(""")
private val NAMED_KEY = Regex("""^key\s*=(?!=)""")
private val NAMED = Regex("""^\s*\w+\s*=(?!=)""")
private val NOT_A_KEY = Regex("""^(?:-?\d|M?R\.)""")
