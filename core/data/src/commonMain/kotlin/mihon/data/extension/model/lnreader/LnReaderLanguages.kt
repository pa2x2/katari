package mihon.data.extension.model.lnreader

/*
 * LNReader indexes name a plugin's language in that language, as its `scripts/languages.js` writes them, some with a
 * left-to-right mark in front. Katari lists sources by language code.
 */

private val CODES = mapOf(
    "English" to "en",
    "Русский" to "ru",
    "العربية" to "ar",
    "Español" to "es",
    "Français" to "fr",
    "Türkçe" to "tr",
    // Every Portuguese plugin is for a Brazilian site
    "Português" to "pt-BR",
    "Bahasa Indonesia" to "id",
    "中文, 汉语, 漢語" to "zh",
    "Tiếng Việt" to "vi",
    "日本語" to "ja",
    "조선말, 한국어" to "ko",
    "ไทย" to "th",
    "Українська" to "uk",
    "বাংলা" to "bn",
    "Polski" to "pl",
    "Multi" to "all",
)

private const val LEFT_TO_RIGHT_MARK = '‎'

/** The language code of an LNReader index's language name; `other` for one it doesn't name. */
internal fun lnReaderLanguageCode(name: String): String {
    return CODES[name.trim { it.isWhitespace() || it == LEFT_TO_RIGHT_MARK }] ?: "other"
}
