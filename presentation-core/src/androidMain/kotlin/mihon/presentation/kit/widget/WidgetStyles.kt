package mihon.presentation.kit.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/*
 * Text and colours of the home-screen widgets, as Glance types. Widget colours otherwise come from the widget's own
 * colour resources, which follow the launcher's dynamic colours rather than the app's theme.
 */

/** Content colour of the cover-screen widget, which always sits on a dark image. */
val WidgetOnCoverScreen: ColorProvider = ColorProvider(Color.White)

/** A widget's message text, such as "no recent updates", in [color]. */
fun widgetMessageStyle(color: ColorProvider): TextStyle = TextStyle(color = color)

/** A short centred note in a small widget, such as the locked notice, in [color]. */
fun widgetNoteStyle(color: ColorProvider): TextStyle = TextStyle(
    color = color,
    fontSize = 12.sp,
    textAlign = TextAlign.Center,
)
