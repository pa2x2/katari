package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.i18n.*

/**
 * Aniyomi's own colour themes aren't written, so Katari keeps its default, and the report lists the theme by its name
 * (D065). Every other theme is Mihon's.
 */
internal fun AniyomiSettings.convertTheme() {
    val theme = (peek(THEME_KEY) as? StringPreferenceValue)?.value ?: return
    val name = ANIYOMI_THEMES[theme] ?: return
    take(THEME_KEY)
    leaveBehind(Item(name, MR.strings.backup_conversion_aniyomi_theme))
}

private const val THEME_KEY = "pref_app_theme"

// Aniyomi's AppTheme names and what its theme picker calls them
private val ANIYOMI_THEMES = mapOf(
    "CLOUDFLARE" to "Cloudflare",
    "COTTONCANDY" to "Cotton Candy",
    "DOOM" to "Doom",
    "MATRIX" to "Matrix",
    "MOCHA" to "Mocha",
    "SAPPHIRE" to "Sapphire",
)
