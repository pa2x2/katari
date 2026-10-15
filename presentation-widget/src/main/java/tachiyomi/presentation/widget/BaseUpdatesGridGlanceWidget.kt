package tachiyomi.presentation.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import mihon.core.metro.metroGraph
import mihon.presentation.kit.widget.WidgetMetrics
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.presentation.widget.components.LockedWidget
import tachiyomi.presentation.widget.components.UpdatesWidget
import tachiyomi.presentation.widget.util.appWidgetBackgroundRadius
import tachiyomi.presentation.widget.util.calculateRowAndColumnCount
import kotlin.time.Clock
import kotlin.time.Instant

abstract class BaseUpdatesGridGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    abstract val foreground: ColorProvider
    abstract val background: ImageProvider
    abstract val topPadding: Dp
    abstract val bottomPadding: Dp

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .appWidgetBackground()
            .padding(top = topPadding, bottom = bottomPadding)
            .appWidgetBackgroundRadius()
        val graph = context.metroGraph<UpdatesWidgetGraph>()

        if (graph.securityPreferences.useAuthenticator.get()) {
            provideContent {
                LockedWidget(
                    foreground = foreground,
                    modifier = containerModifier,
                )
            }
        }

        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(javaClass)
        val (rowCount, columnCount) = ids
            .flatMap { manager.getAppWidgetSizes(it) }
            .maxBy { it.height.value * it.width.value }
            .calculateRowAndColumnCount(topPadding, bottomPadding)

        provideContent {
            val flow = remember {
                graph.getUpdates
                    .subscribe(false, DateLimit.toEpochMilliseconds())
                    .map { rawData ->
                        rawData.prepareData(context.applicationContext, rowCount, columnCount)
                    }
            }
            val data by flow.collectAsState(initial = null)
            UpdatesWidget(
                data = data,
                contentColor = foreground,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                modifier = containerModifier,
            )
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun List<UpdatesWithRelations>.prepareData(
        context: Context,
        rowCount: Int,
        columnCount: Int,
    ): List<Pair<Long, Bitmap?>> {
        // Resize to cover size
        val widthPx = WidgetMetrics.coverWidth.value.toInt().dpToPx
        val heightPx = WidgetMetrics.coverHeight.value.toInt().dpToPx
        val roundPx = context.resources.getDimension(R.dimen.appwidget_inner_radius)
        return withContext(Dispatchers.Default) {
            this@prepareData
                .distinctBy { it.entryId }
                .take(rowCount * columnCount)
                .map { updatesView ->
                    val request = ImageRequest.Builder(context)
                        .data(
                            EntryCover(
                                entryId = updatesView.entryId,
                                sourceId = updatesView.sourceId,
                                isFavorite = true,
                                url = updatesView.coverData.url,
                                lastModified = updatesView.coverData.lastModified,
                            ),
                        )
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .precision(Precision.EXACT)
                        .size(widthPx, heightPx)
                        .scale(Scale.FILL)
                        .let {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                it.transformations(RoundedCornersTransformation(roundPx))
                            } else {
                                it // Handled by system
                            }
                        }
                        .build()
                    val bitmap = context.imageLoader.execute(request)
                        .image
                        ?.asDrawable(context.resources)
                        ?.toBitmap()
                    Pair(updatesView.entryId, bitmap)
                }
        }
    }

    companion object {
        val DateLimit: Instant
            get() = Clock.System.now().minus(3, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
    }
}
