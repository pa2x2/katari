package mihon.text.recognition.ui.language

import androidx.compose.runtime.Composable
import mihon.language.api.tag.LanguageTag
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

/** The language's name in the app language, for recognition settings and prompts. */
fun LanguageTag.displayName(locale: Locale = Locale.getDefault()): String =
    Locale.forLanguageTag(value).getDisplayName(locale).ifBlank { value }

fun Collection<LanguageTag>.displayNames(): String = joinToString { it.displayName() }

/** Up to three language names, then how many more there are, for one-line labels. */
@Composable
fun List<LanguageTag>.shortDisplayNames(): String {
    val shown = take(NAMED_LANGUAGES).joinToString { it.displayName() }
    val more = size - NAMED_LANGUAGES
    return if (more > 0) stringResource(MR.strings.text_recognition_languages_more, shown, more) else shown
}

private const val NAMED_LANGUAGES = 3
