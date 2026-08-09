package mihon.translation.ui.picker.language

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguageSupport
import java.text.Collator
import java.util.Locale

data class TranslationLanguageOption(
    val tag: LanguageTag,
    val displayName: String,
)

fun translationLanguageOptions(
    availableLocales: Array<Locale> = Locale.getAvailableLocales(),
    displayLocale: Locale = Locale.getDefault(),
): List<TranslationLanguageOption> {
    val collator = Collator.getInstance(displayLocale)
    return availableLocales
        .mapNotNull { locale ->
            val candidate = if (locale.script.isBlank()) locale.language else "${locale.language}-${locale.script}"
            LanguageTag.parse(candidate)
        }
        .distinctBy(LanguageTag::value)
        .map { tag ->
            TranslationLanguageOption(
                tag = tag,
                displayName = Locale.forLanguageTag(tag.value)
                    .getDisplayName(displayLocale)
                    .ifBlank { tag.value },
            )
        }
        .sortedWith { first, second ->
            collator.compare(first.displayName, second.displayName)
                .takeUnless { it == 0 }
                ?: first.tag.value.compareTo(second.tag.value)
        }
}

fun translationLanguageOptions(
    support: TranslationLanguageSupport,
    role: TranslationLanguageRole,
    counterpart: LanguageTag?,
    availableLocales: Array<Locale> = Locale.getAvailableLocales(),
    displayLocale: Locale = Locale.getDefault(),
): List<TranslationLanguageOption> {
    if (support == TranslationLanguageSupport.AnyLanguage) {
        return translationLanguageOptions(availableLocales, displayLocale)
    }
    return translationLanguageOptions(
        tags = support.selectableLanguages(role, counterpart),
        displayLocale = displayLocale,
    )
}

fun translationLanguageOptions(
    tags: Set<LanguageTag>,
    displayLocale: Locale = Locale.getDefault(),
): List<TranslationLanguageOption> {
    val collator = Collator.getInstance(displayLocale)
    return tags
        .map { tag ->
            TranslationLanguageOption(
                tag = tag,
                displayName = Locale.forLanguageTag(tag.value)
                    .getDisplayName(displayLocale)
                    .ifBlank { tag.value },
            )
        }
        .sortedWith { first, second ->
            collator.compare(first.displayName, second.displayName)
                .takeUnless { it == 0 }
                ?: first.tag.value.compareTo(second.tag.value)
        }
}

fun TranslationLanguageSupport.selectableLanguages(
    role: TranslationLanguageRole,
    counterpart: LanguageTag?,
): Set<LanguageTag> {
    return when (this) {
        is TranslationLanguageSupport.ExactPairs -> when (role) {
            TranslationLanguageRole.Source ->
                pairs
                    .mapTo(mutableSetOf(), { it.source })
            TranslationLanguageRole.Target ->
                pairs
                    .filter { counterpart == null || it.source == counterpart }
                    .mapTo(mutableSetOf(), { it.target })
                    .ifEmpty { pairs.mapTo(mutableSetOf(), { it.target }) }
        }
        is TranslationLanguageSupport.ByRole -> when (role) {
            TranslationLanguageRole.Source -> sourceLanguages
            TranslationLanguageRole.Target -> targetLanguages
        }
        TranslationLanguageSupport.AnyLanguage -> emptySet()
    }
}

fun TranslationLanguageSupport.supportsPair(
    source: LanguageTag,
    target: LanguageTag,
): Boolean {
    if (source == target) return false
    return when (this) {
        is TranslationLanguageSupport.ExactPairs ->
            pairs.any { it.source == source && it.target == target }
        is TranslationLanguageSupport.ByRole ->
            source in sourceLanguages && target in targetLanguages
        TranslationLanguageSupport.AnyLanguage -> true
    }
}

fun TranslationLanguageSupport.supportsSelection(
    role: TranslationLanguageRole,
    language: LanguageTag,
    counterpart: LanguageTag?,
): Boolean {
    return this == TranslationLanguageSupport.AnyLanguage ||
        language in selectableLanguages(role, counterpart)
}

enum class TranslationLanguageRole {
    Source,
    Target,
}

fun LanguageTag.displayName(
    locale: Locale = Locale.getDefault(),
): String = Locale.forLanguageTag(value)
    .getDisplayName(locale)
    .ifBlank { value }
