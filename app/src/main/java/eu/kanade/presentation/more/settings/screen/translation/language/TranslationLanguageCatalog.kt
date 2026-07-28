package eu.kanade.presentation.more.settings.screen.translation.language

import mihon.translation.api.TranslationLanguageTag
import java.text.Collator
import java.util.Locale

internal data class TranslationLanguageOption(
    val tag: TranslationLanguageTag,
    val displayName: String,
)

internal fun translationLanguageOptions(
    availableLocales: Array<Locale> = Locale.getAvailableLocales(),
    displayLocale: Locale = Locale.getDefault(),
): List<TranslationLanguageOption> {
    val collator = Collator.getInstance(displayLocale)
    return availableLocales
        .mapNotNull { locale ->
            val candidate = if (locale.script.isBlank()) {
                locale.language
            } else {
                "${locale.language}-${locale.script}"
            }
            TranslationLanguageTag.parse(candidate)
        }
        .distinctBy(TranslationLanguageTag::value)
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

internal fun TranslationLanguageTag.displayName(
    locale: Locale = Locale.getDefault(),
): String {
    return Locale.forLanguageTag(value)
        .getDisplayName(locale)
        .ifBlank { value }
}
