package tachiyomi.presentation.widget.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import mihon.presentation.kit.widget.WidgetMetrics
import mihon.presentation.kit.widget.widgetNoteStyle
import tachiyomi.core.common.Constants
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

@Composable
fun LockedWidget(
    foreground: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val intent = Intent(context, Class.forName(Constants.MAIN_ACTIVITY)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    Box(
        modifier = modifier
            .clickable(actionStartActivity(intent))
            .padding(WidgetMetrics.messageInset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.stringResource(MR.strings.appwidget_unavailable_locked),
            style = widgetNoteStyle(foreground),
        )
    }
}
