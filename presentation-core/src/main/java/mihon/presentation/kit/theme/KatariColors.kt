package mihon.presentation.kit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct

/**
 * The colour roles UI code may use: the Material roles of the current scheme, plus roles Material has no slot for.
 *
 * Status, filter and chart colours start from fixed seeds and are blended toward the scheme's primary, so a green
 * still reads as success in Nord or Strawberry without clashing with it.
 */
@Immutable
class KatariColors internal constructor(scheme: ColorScheme, isDark: Boolean) {
    val primary: Color = scheme.primary
    val onPrimary: Color = scheme.onPrimary
    val primaryContainer: Color = scheme.primaryContainer
    val onPrimaryContainer: Color = scheme.onPrimaryContainer
    val inversePrimary: Color = scheme.inversePrimary
    val secondary: Color = scheme.secondary
    val onSecondary: Color = scheme.onSecondary
    val secondaryContainer: Color = scheme.secondaryContainer
    val onSecondaryContainer: Color = scheme.onSecondaryContainer
    val tertiary: Color = scheme.tertiary
    val onTertiary: Color = scheme.onTertiary
    val tertiaryContainer: Color = scheme.tertiaryContainer
    val onTertiaryContainer: Color = scheme.onTertiaryContainer
    val error: Color = scheme.error
    val onError: Color = scheme.onError
    val errorContainer: Color = scheme.errorContainer
    val onErrorContainer: Color = scheme.onErrorContainer
    val background: Color = scheme.background
    val onBackground: Color = scheme.onBackground
    val surface: Color = scheme.surface
    val onSurface: Color = scheme.onSurface
    val surfaceVariant: Color = scheme.surfaceVariant
    val onSurfaceVariant: Color = scheme.onSurfaceVariant
    val surfaceTint: Color = scheme.surfaceTint
    val inverseSurface: Color = scheme.inverseSurface
    val inverseOnSurface: Color = scheme.inverseOnSurface
    val outline: Color = scheme.outline
    val outlineVariant: Color = scheme.outlineVariant
    val surfaceBright: Color = scheme.surfaceBright
    val surfaceDim: Color = scheme.surfaceDim
    val surfaceContainerLowest: Color = scheme.surfaceContainerLowest
    val surfaceContainerLow: Color = scheme.surfaceContainerLow
    val surfaceContainer: Color = scheme.surfaceContainer
    val surfaceContainerHigh: Color = scheme.surfaceContainerHigh
    val surfaceContainerHighest: Color = scheme.surfaceContainerHighest

    val success: Color
    val onSuccess: Color
    val successContainer: Color
    val onSuccessContainer: Color

    val warning: Color
    val onWarning: Color
    val warningContainer: Color
    val onWarningContainer: Color

    /** Tint of a toolbar action whose filter is applied. */
    val active: Color

    /** Series, tracks, rules and hatching of charts. Series accents keep the order statistics assigns them. */
    val chart: ChartColors

    /** Text and icons drawn over video or page images, whatever the theme. */
    val onMedia: Color = MEDIA_LIGHT

    /** Secondary text and icons over video or page images. */
    val onMediaVariant: Color = MEDIA_LIGHT.copy(alpha = MEDIA_VARIANT_ALPHA)

    /** Shade laid over video or page images behind controls. */
    val mediaScrim: Color = MEDIA_DARK.copy(alpha = MEDIA_SCRIM_ALPHA)

    /** The darkest part of a controls shade, right at the image edge. */
    val mediaScrimEdge: Color = MEDIA_DARK.copy(alpha = MEDIA_SCRIM_EDGE_ALPHA)

    /**
     * Reader bars and controls laid over pages: the raised surface, slightly see-through so the page stays in view.
     * Content on it uses the normal surface colours.
     */
    val chrome: Color = scheme.surfaceContainer.copy(alpha = if (isDark) CHROME_DARK_ALPHA else CHROME_LIGHT_ALPHA)

    /** Background behind video and pages. */
    val media: Color = MEDIA_DARK

    /** Shade behind modal sheets and dialogs. */
    val scrim: Color = scheme.scrim.copy(alpha = SCRIM_ALPHA)

    /** Fill of an image that has not loaded or does not exist. */
    val placeholder: Color = scheme.onSurface.copy(alpha = PLACEHOLDER_ALPHA)

    /** Fill over an area the user is outlining on a page or image; what lies under it stays visible. */
    val selectionFill: Color = scheme.primary.copy(alpha = SELECTION_FILL_ALPHA)

    /** Marks drawn over a page or image, such as an outlined area or numbered regions. */
    val markup: MarkupColors = MarkupColors(shade = scrim, fill = selectionFill, mark = primary, onMark = onPrimary)

    /** Background of a selected list item or grid cell. */
    val selected: Color = scheme.secondaryContainer

    /** Content of a disabled control. Kit components apply it themselves through their enabled state. */
    val disabledContent: Color = scheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)

    /** Container of a disabled control. */
    val disabledContainer: Color = scheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)

    init {
        val primary = scheme.primary
        val success = ToneSet.of(SUCCESS_SEED.harmonize(primary), isDark)
        this.success = success.accent
        onSuccess = success.onAccent
        successContainer = success.container
        onSuccessContainer = success.onContainer
        val warning = ToneSet.of(WARNING_SEED.harmonize(primary), isDark)
        this.warning = warning.accent
        onWarning = warning.onAccent
        warningContainer = warning.container
        onWarningContainer = warning.onContainer
        active = ToneSet.of(ACTIVE_SEED.harmonize(primary), isDark).accent
        chart = ChartColors(scheme, CHART_SEEDS.map { seed -> ToneSet.of(seed.harmonize(primary), isDark).accent })
    }
}

internal val LocalKatariColors = staticCompositionLocalOf<KatariColors> {
    error("KatariColors are provided by KatariTheme")
}

/** Material's tone pairs for a custom role, taken from one harmonized seed. */
private class ToneSet(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color) {
    companion object {
        fun of(seed: Color, isDark: Boolean): ToneSet {
            val hct = seed.toHct()
            val chroma = hct.chroma.coerceAtLeast(MIN_ROLE_CHROMA)
            fun tone(tone: Double) = Hct.from(hct.hue, chroma, tone).toColor()
            return if (isDark) {
                ToneSet(tone(80.0), tone(20.0), tone(30.0), tone(90.0))
            } else {
                ToneSet(tone(40.0), tone(100.0), tone(90.0), tone(10.0))
            }
        }
    }
}

private const val MIN_ROLE_CHROMA = 36.0
private const val SCRIM_ALPHA = 0.32f
private const val PLACEHOLDER_ALPHA = 0.12f
private const val SELECTION_FILL_ALPHA = 0.2f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val MEDIA_VARIANT_ALPHA = 0.7f
private const val MEDIA_SCRIM_ALPHA = 0.6f
private const val MEDIA_SCRIM_EDGE_ALPHA = 0.8f
private const val CHROME_DARK_ALPHA = 0.9f
private const val CHROME_LIGHT_ALPHA = 0.95f

private val MEDIA_LIGHT = Color.White
private val MEDIA_DARK = Color.Black
private val SUCCESS_SEED = Color(0xFF4CAF50)
private val WARNING_SEED = Color(0xFFFFB300)
private val ACTIVE_SEED = Color(0xFFFFC107)

// Rose, sky, sage, plum and amber: the hues statistics used before they followed the theme.
private val CHART_SEEDS = listOf(
    Color(0xFFAD496F),
    Color(0xFF2478B2),
    Color(0xFF4D8248),
    Color(0xFF7352A6),
    Color(0xFF95600C),
)
