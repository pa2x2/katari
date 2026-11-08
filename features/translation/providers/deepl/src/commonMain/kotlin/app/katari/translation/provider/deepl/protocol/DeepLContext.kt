package app.katari.translation.provider.deepl.protocol

import app.katari.translation.api.request.TranslationContext

/** Writes [TranslationContext] as the free text the DeepL API reads as context; null when there is none. */
internal fun TranslationContext.toDeepLContext(): String? {
    val sections = buildList {
        work?.let { work ->
            add(
                listOfNotNull(
                    "Title: ${work.title}",
                    work.description?.takeIf(String::isNotBlank)?.let { "Description: $it" },
                ).joinToString("\n"),
            )
        }
        if (precedingText.isNotEmpty()) {
            add("Preceding text:\n" + precedingText.joinToString("\n"))
        }
    }
    return sections.joinToString("\n\n").takeIf(String::isNotBlank)
}
