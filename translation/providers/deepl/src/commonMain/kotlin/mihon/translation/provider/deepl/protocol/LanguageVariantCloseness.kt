package mihon.translation.provider.deepl.protocol

import java.util.Locale

/**
 * How well this variant of a language stands in for the [requested] one of the same language; the higher, the
 * closer. The script outweighs the region: text in another script cannot be read, text from another region can.
 */
internal fun Locale.closenessTo(requested: Locale): Int {
    val script = if (writtenScript() == requested.writtenScript()) SAME_SCRIPT else 0
    val region = when {
        country.isEmpty() -> ANY_REGION
        country == requested.country -> SAME_REGION
        country == LATIN_AMERICA && requested.country in LATIN_AMERICAN_REGIONS -> CONTAINING_REGION
        else -> 0
    }
    return script + region
}

/** The script of the tag, or the one its language is written in where the tag names none. */
private fun Locale.writtenScript(): String = when {
    script.isNotEmpty() -> script
    language == "zh" -> if (country in TRADITIONAL_CHINESE_REGIONS) "Hant" else "Hans"
    else -> ""
}

private const val SAME_SCRIPT = 4
private const val SAME_REGION = 3
private const val CONTAINING_REGION = 2
private const val ANY_REGION = 1

/** Where Chinese is written in traditional characters; elsewhere it is written in simplified ones. */
private val TRADITIONAL_CHINESE_REGIONS = setOf("HK", "MO", "TW")

/** The UN M.49 code for Latin America and the Caribbean, which names a variant as a region does, as in `es-419`. */
private const val LATIN_AMERICA = "419"

/** The regions whose usage CLDR derives from that of Latin America. */
private val LATIN_AMERICAN_REGIONS = setOf(
    "AR", "BO", "BR", "BZ", "CL", "CO", "CR", "CU", "DO", "EC", "GT",
    "HN", "MX", "NI", "PA", "PE", "PR", "PY", "SV", "US", "UY", "VE",
)
