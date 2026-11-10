package app.katari.desktop.security.pam

import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * The modules [service]'s `auth` stack can run, by file name such as `pam_unix.so`, through the services it includes
 * (`include` and `substack`, and Debian's `@include`). Whether a module runs at all depends on the ones before it, so
 * this says what the stack may ask for, not what a given sign-in will.
 */
internal fun pamAuthModules(service: String, configDirectories: List<Path> = PAM_CONFIG_DIRECTORIES): Set<String> =
    buildSet { addAuthModules(service, configDirectories, visited = mutableSetOf()) }

private fun MutableSet<String>.addAuthModules(
    service: String,
    configDirectories: List<Path>,
    visited: MutableSet<String>,
) {
    if (!visited.add(service)) return
    val file = pamServiceFile(service, configDirectories) ?: return
    for (fields in pamRules(file)) {
        if (fields.first() == "@include") {
            fields.getOrNull(1)?.let { addAuthModules(it, configDirectories, visited) }
            continue
        }
        // A leading `-` only keeps a missing module from being logged
        if (fields.size < 3 || !fields[0].removePrefix("-").equals("auth", ignoreCase = true)) continue
        when (fields[1]) {
            "include", "substack" -> addAuthModules(fields[2], configDirectories, visited)
            else -> add(fields[2].substringAfterLast('/'))
        }
    }
}

/**
 * The rules of a PAM service file, each split into its fields: comments dropped, lines ending in `\` joined to the
 * next, and a bracketed control such as `[success=1 default=ignore]` kept as one field.
 */
private fun pamRules(file: Path): List<List<String>> {
    val rules = mutableListOf<List<String>>()
    var pending = ""
    for (line in file.readLines()) {
        val text = pending + line.substringBefore('#')
        if (text.endsWith('\\')) {
            pending = text.dropLast(1) + " "
            continue
        }
        pending = ""
        FIELD.findAll(text).map { it.value }.toList().takeIf { it.isNotEmpty() }?.let(rules::add)
    }
    return rules
}

private val FIELD = Regex("""\[[^\]]*]|\S+""")
