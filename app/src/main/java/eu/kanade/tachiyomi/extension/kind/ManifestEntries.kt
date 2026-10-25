package eu.kanade.tachiyomi.extension.kind

import mihon.domain.extension.model.ContentWarning

/*
 * Manifest entries every ecosystem forked from Tachiyomi writes the same way, whatever it names their keys.
 */

/** The content warning a manifest's level stands for: 1 mixed, 2 NSFW, anything else safe. */
internal fun contentWarningOf(level: Int): ContentWarning {
    return when (level) {
        1 -> ContentWarning.MIXED
        2 -> ContentWarning.NSFW
        else -> ContentWarning.SAFE
    }
}

/** The `;`-separated class names of a manifest entry, a leading `.` meaning one in [packageName]. */
internal fun sourceClassNames(packageName: String, declared: String?): List<String> {
    if (declared.isNullOrBlank()) return emptyList()
    return declared.split(";").map {
        val sourceClass = it.trim()
        if (sourceClass.startsWith(".")) packageName + sourceClass else sourceClass
    }
}
