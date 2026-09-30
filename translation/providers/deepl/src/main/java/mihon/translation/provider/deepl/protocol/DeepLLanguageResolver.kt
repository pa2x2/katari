package mihon.translation.provider.deepl.protocol

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.request.ResolvedTranslationRoute
import mihon.translation.provider.server.engine.ServerLanguageCodes
import mihon.translation.provider.server.engine.ServerLanguages
import java.util.Locale

/**
 * Names languages the way a server does.
 *
 * A language the server lists is named exactly. Any other variant of a listed language is named by the one variant
 * the server has, or, when it has several (as with `EN-GB` and `EN-US`), by the bare language code, which the DeepL
 * API accepts for every language it lists variants of.
 */
internal class DeepLLanguageResolver(
    languages: DeepLLanguages,
) : ServerLanguages {
    private val sources = languages.sources.tagged()
    private val targets = languages.targets.tagged()

    override val isEmpty = languages.sources.isEmpty() || languages.targets.isEmpty()

    fun source(language: LanguageTag): String? = resolve(language, sources)

    fun target(language: LanguageTag): String? = resolve(language, targets)

    override fun codes(route: ResolvedTranslationRoute): ServerLanguageCodes? {
        return ServerLanguageCodes(
            source = source(route.sourceLanguage) ?: return null,
            target = target(route.targetLanguage) ?: return null,
        )
    }

    override fun languageSupport(): TranslationLanguageSupportInspection {
        if (sources.isEmpty() || targets.isEmpty()) {
            return TranslationLanguageSupportInspection.Unavailable("Server reported no supported languages")
        }
        return TranslationLanguageSupportInspection.Available(
            TranslationLanguageSupport.ByRole(
                sourceLanguages = sources.resolvable(),
                targetLanguages = targets.resolvable(),
            ),
        )
    }

    private fun resolve(language: LanguageTag, listed: Map<LanguageTag, String>): String? {
        listed[language]?.let { return it }
        val variants = listed.filterKeys { it.baseLanguage() == language.baseLanguage() }
        return when (variants.size) {
            0 -> null
            1 -> variants.values.single()
            else -> language.baseLanguage().uppercase(Locale.ROOT)
        }
    }

    /** The listed languages and the bare languages they are variants of, each of which resolves to a code. */
    private fun Map<LanguageTag, String>.resolvable(): Set<LanguageTag> =
        keys + keys.mapNotNull { LanguageTag.parse(it.baseLanguage()) }

    private fun List<DeepLLanguage>.tagged(): Map<LanguageTag, String> =
        mapNotNull { language -> LanguageTag.parse(language.code)?.let { it to language.code } }.toMap()

    private fun LanguageTag.baseLanguage(): String = Locale.forLanguageTag(value).language
}
