package mihon.translation.provider.server.engine

import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.request.ResolvedTranslationRoute

/** The languages a server reported, read the way its provider names languages. */
interface ServerLanguages {
    /** Whether the server reported nothing it could translate. */
    val isEmpty: Boolean

    fun languageSupport(): TranslationLanguageSupportInspection

    /** What the server calls the languages of [route], or null when it does not translate between them. */
    fun codes(route: ResolvedTranslationRoute): ServerLanguageCodes?
}

data class ServerLanguageCodes(
    val source: String,
    val target: String,
)
