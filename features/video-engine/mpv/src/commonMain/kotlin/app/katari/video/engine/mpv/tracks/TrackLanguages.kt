package app.katari.video.engine.mpv.tracks

import java.util.Locale
import java.util.MissingResourceException

/*
 * A track's language as ExoPlayer normalises it (Media3's Util.normalizeLanguageCode), so a track mpv lists carries
 * the language the same track has on ExoPlayer. Saved subtitle choices are keyed by it, and mpv reports a file's
 * ISO 639-2 codes (`eng`) where ExoPlayer has the two-letter ones (`en`).
 *
 * The tag is lower-cased with '-' between its parts; a three-letter code with a two-letter one becomes it, and
 * deprecated codes and legacy tags become their current ones. Media3 builds the table from the same Locale data, so on
 * Android both read the same.
 */

internal fun normalizedLanguageTag(language: String?): String? {
    if (language == null) return null
    var tag = language.replace('_', '-').takeUnless { it.isEmpty() || it == UNDETERMINED } ?: language
    tag = tag.asciiLowercase()
    var mainLanguage = tag.substringBefore('-')
    LANGUAGE_REPLACEMENTS[mainLanguage]?.let { replaced ->
        tag = replaced + tag.substring(mainLanguage.length)
        mainLanguage = replaced
    }
    if (mainLanguage == "no" || mainLanguage == "i" || mainLanguage == "zh") {
        LEGACY_TAG_REPLACEMENTS.firstOrNull { (legacy, _) -> tag.startsWith(legacy) }?.let { (legacy, current) ->
            tag = current + tag.substring(legacy.length)
        }
    }
    return tag
}

private const val UNDETERMINED = "und"

// Only ASCII, as Media3 lowers it: a tag in another script stays as written.
private fun String.asciiLowercase(): String = buildString(length) {
    this@asciiLowercase.forEach { append(if (it in 'A'..'Z') it + ('a' - 'A') else it) }
}

private val LANGUAGE_REPLACEMENTS: Map<String, String> by lazy {
    buildMap {
        Locale.getISOLanguages().forEach { iso2 ->
            val iso3 = try {
                Locale.forLanguageTag(iso2).isO3Language
            } catch (_: MissingResourceException) {
                null
            }
            if (!iso3.isNullOrEmpty()) put(iso3, iso2)
        }
        putAll(ADDITIONAL_REPLACEMENTS)
    }
}

private val ADDITIONAL_REPLACEMENTS = listOf(
    // ISO 639-2/B bibliographic codes, replaced by the ISO 639-2/T terminological ones.
    "alb" to "sq",
    "arm" to "hy",
    "baq" to "eu",
    "bur" to "my",
    "tib" to "bo",
    "chi" to "zh",
    "cze" to "cs",
    "dut" to "nl",
    "ger" to "de",
    "gre" to "el",
    "fre" to "fr",
    "geo" to "ka",
    "ice" to "is",
    "mac" to "mk",
    "mao" to "mi",
    "may" to "ms",
    "per" to "fa",
    "rum" to "ro",
    "scc" to "hbs-srp",
    "slo" to "sk",
    "wel" to "cy",
    // Deprecated two-letter codes, replaced by their current ones, macrolanguage included.
    "id" to "ms-ind",
    "iw" to "he",
    "heb" to "he",
    "ji" to "yi",
    // Individual languages, mapped back to their macrolanguage.
    "arb" to "ar-arb",
    "in" to "ms-ind",
    "ind" to "ms-ind",
    "nb" to "no-nob",
    "nob" to "no-nob",
    "nn" to "no-nno",
    "nno" to "no-nno",
    "tw" to "ak-twi",
    "twi" to "ak-twi",
    "bs" to "hbs-bos",
    "bos" to "hbs-bos",
    "hr" to "hbs-hrv",
    "hrv" to "hbs-hrv",
    "sr" to "hbs-srp",
    "srp" to "hbs-srp",
    "cmn" to "zh-cmn",
    "hak" to "zh-hak",
    "nan" to "zh-nan",
    "hsn" to "zh-hsn",
)

private val LEGACY_TAG_REPLACEMENTS = listOf(
    "i-lux" to "lb",
    "i-hak" to "zh-hak",
    "i-navajo" to "nv",
    "no-bok" to "no-nob",
    "no-nyn" to "no-nno",
    "zh-guoyu" to "zh-cmn",
    "zh-hakka" to "zh-hak",
    "zh-min-nan" to "zh-nan",
    "zh-xiang" to "zh-hsn",
)
