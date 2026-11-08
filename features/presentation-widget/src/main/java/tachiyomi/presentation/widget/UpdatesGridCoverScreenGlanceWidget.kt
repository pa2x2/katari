package tachiyomi.presentation.widget

import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import app.katari.presentation.kit.widget.WidgetMetrics
import app.katari.presentation.kit.widget.WidgetOnCoverScreen

class UpdatesGridCoverScreenGlanceWidget : BaseUpdatesGridGlanceWidget() {
    override val foreground = WidgetOnCoverScreen
    override val background = ImageProvider(R.drawable.appwidget_coverscreen_background)
    override val topPadding = 0.dp
    override val bottomPadding = WidgetMetrics.coverScreenBottomInset
}
