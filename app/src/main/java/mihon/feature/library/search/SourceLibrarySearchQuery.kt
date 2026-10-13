package mihon.feature.library.search

import tachiyomi.domain.source.model.SourceDisplayInfo

/**
 * The library search for one source's entries. It uses the `source` field, because a query starting with `src:` is
 * read as an exact source id.
 */
fun SourceDisplayInfo.librarySearchQuery(): String {
    if (!hasKnownName) return "srcid:$id"
    val quotedName = when {
        '"' !in name -> "\"$name\""
        '\'' !in name -> "'$name'"
        else -> return "srcid:$id"
    }
    return if (lang.isBlank()) "source:$quotedName" else "source:$quotedName lang:$lang"
}
