package eu.kanade.tachiyomi.util.system

import eu.kanade.domain.source.interactor.SourceListState
import mihon.core.platform.PlatformContext
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.util.Locale

/**
 * Names of source and app languages, and the order sources are grouped by language in.
 */
object LocaleHelper {

    /**
     * Sorts by display name, except keeps the "all" (displayed as "Multi") locale at the top.
     */
    val comparator = { a: String, b: String ->
        if (a == "all") {
            -1
        } else if (b == "all") {
            1
        } else {
            getLocalizedDisplayName(a).compareTo(getLocalizedDisplayName(b))
        }
    }

    /**
     * Returns display name of a string language code.
     */
    fun getSourceDisplayName(lang: String?, context: PlatformContext): String {
        return when (lang) {
            SourceListState.LAST_USED_KEY -> context.stringResource(MR.strings.last_used_source)
            SourceListState.PINNED_KEY -> context.stringResource(MR.strings.pinned_sources)
            "other" -> context.stringResource(MR.strings.other_source)
            "all" -> context.stringResource(MR.strings.multi_lang)
            else -> getLocalizedDisplayName(lang)
        }
    }

    fun getDisplayName(lang: String): String {
        val normalizedLang = when (lang) {
            "zh-CN" -> "zh-Hans"
            "zh-TW" -> "zh-Hant"
            else -> lang
        }

        return Locale.forLanguageTag(normalizedLang).displayName
    }

    fun getShortDisplayName(lang: String?, uppercase: Boolean = false): String {
        return when (lang) {
            null -> ""
            "es-419" -> "es-la"
            "zh-CN" -> "zh-hans"
            "zh-TW" -> "zh-hant"
            else -> lang
        }
            .let { if (uppercase) it.uppercase(Locale.ENGLISH) else it }
    }

    /**
     * Returns display name of a string language code.
     *
     * @param lang empty for system language
     */
    fun getLocalizedDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = when (lang) {
            "" -> Locale.getDefault()
            "zh-CN" -> Locale.forLanguageTag("zh-Hans")
            "zh-TW" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(lang)
        }
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }
}
