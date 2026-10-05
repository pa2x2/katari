package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.recap.StatisticsRecapCard
import eu.kanade.presentation.more.stats.recap.StatisticsRecapPreview
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/**
 * A recap of one period, shown as the image it's shared as.
 *
 * @param startLocalDate first day of the period, or null to start at the first recorded activity.
 * @param periodLabel how the period reads on the image, such as "September 2026".
 */
data class StatisticsRecapScreen(
    private val startLocalDate: String?,
    private val endLocalDate: String,
    private val typeName: String?,
    private val periodLabel: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { StatisticsRecapScreenModel(startLocalDate, endLocalDate, typeName) }
        val state by screenModel.state.collectAsState()
        val typeLabel = screenModel.type?.let { stringResource(it.entryTypePresentation().displayNameLabel) }
        val graphicsLayer = rememberGraphicsLayer()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_recap),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            when (val current = state) {
                StatisticsRecapScreenModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
                StatisticsRecapScreenModel.State.Failed -> EmptyScreen(
                    stringRes = MR.strings.statistics_could_not_load_activity,
                    modifier = Modifier.padding(paddingValues),
                )
                is StatisticsRecapScreenModel.State.Success -> {
                    if (current.recap.totalDurationMillis <= 0L) {
                        EmptyScreen(
                            stringRes = MR.strings.statistics_empty_period,
                            modifier = Modifier.padding(paddingValues),
                        )
                        return@Scaffold
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StatisticsRecapPreview(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            StatisticsRecapCard(
                                recap = current.recap,
                                periodLabel = periodLabel,
                                typeLabel = typeLabel,
                                modifier = Modifier.drawWithContent {
                                    graphicsLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(graphicsLayer)
                                },
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                        val file = screenModel.writeImage(bitmap, context.cacheImageDir)
                                        context.startActivity(
                                            file.getUriCompat(context).toShareIntent(context, type = "image/png"),
                                        )
                                    } catch (error: Exception) {
                                        logcat(LogPriority.ERROR, error)
                                        context.toast(MR.strings.statistics_recap_share_failed)
                                    }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(MR.strings.action_share))
                        }
                    }
                }
            }
        }
    }
}
