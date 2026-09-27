package mihon.text.recognition.ui.language

import mihon.language.api.tag.LanguageTag
import java.util.Locale

/** The language's name in the app language, for recognition settings and prompts. */
fun LanguageTag.displayName(locale: Locale = Locale.getDefault()): String =
    Locale.forLanguageTag(value).getDisplayName(locale).ifBlank { value }

fun Collection<LanguageTag>.displayNames(): String = joinToString { it.displayName() }
