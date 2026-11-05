package mihon.presentation.kit.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.style
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What tapping a zone does, which picks its colour and label. */
enum class TapZoneRole { Menu, Previous, Next, Left, Right }

/** A tap zone as fractions of the area it covers, from its top-left corner. */
@Immutable
data class TapZone(val left: Float, val top: Float, val right: Float, val bottom: Float, val role: TapZoneRole)

/**
 * Shows where the tap [zones] of a reader are: each zone tinted by its role and labelled with it, fading in when
 * [visible] turns true and out when it turns false. It takes no input, so the reader below keeps every touch.
 */
@Composable
fun TapZoneOverlay(zones: List<TapZone>, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && zones.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn(tween(FADE_MILLIS)),
        exit = fadeOut(tween(FADE_MILLIS)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            zones.forEach { zone ->
                Box(
                    modifier = Modifier
                        .offset(maxWidth * zone.left, maxHeight * zone.top)
                        .size(maxWidth * (zone.right - zone.left), maxHeight * (zone.bottom - zone.top))
                        .background(zone.role.color),
                    contentAlignment = Alignment.Center,
                ) {
                    ZoneLabel(stringResource(zone.role.label))
                }
            }
        }
    }
}

// Light text with a dark outline reads on every zone colour and on any page showing through
@Composable
private fun ZoneLabel(text: String) {
    val style = TextRole.DetailTitle.style(MaterialTheme.typography)
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.Black,
            textAlign = TextAlign.Center,
            style = style.copy(drawStyle = Stroke(width = OUTLINE_WIDTH)),
        )
        Text(text = text, color = Color.White, textAlign = TextAlign.Center, style = style)
    }
}

private val TapZoneRole.color: Color
    get() = when (this) {
        TapZoneRole.Menu -> Color(0xCC95818D)
        TapZoneRole.Previous -> Color(0xCCFF7733)
        TapZoneRole.Next -> Color(0xCC84E296)
        TapZoneRole.Left -> Color(0xCC7D1128)
        TapZoneRole.Right -> Color(0xCCA6CFD5)
    }

private val TapZoneRole.label
    get() = when (this) {
        TapZoneRole.Menu -> MR.strings.action_menu
        TapZoneRole.Previous -> MR.strings.nav_zone_prev
        TapZoneRole.Next -> MR.strings.nav_zone_next
        TapZoneRole.Left -> MR.strings.nav_zone_left
        TapZoneRole.Right -> MR.strings.nav_zone_right
    }

private const val FADE_MILLIS = 1000
private const val OUTLINE_WIDTH = 8f
