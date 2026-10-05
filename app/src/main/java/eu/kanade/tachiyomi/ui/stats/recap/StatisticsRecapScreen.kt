package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import eu.kanade.presentation.more.stats.recap.story.RecapStoryPlayer
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/** A recap of [period]: a story for years and months, the summary card alone for any other window. */
data class StatisticsRecapScreen(private val period: StatisticsRecapPeriod) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { StatisticsRecapScreenModel(period) }
        val state by screenModel.state.collectAsState()
        val seeds by screenModel.coverSeeds.collectAsState()
        val title = recapPeriodTitle(period)
        val footer = "${stringResource(MR.strings.app_name)} · $title"

        when (val current = state) {
            StatisticsRecapScreenModel.State.Loading -> LoadingScreen()
            StatisticsRecapScreenModel.State.Failed -> RecapMessage(MR.strings.statistics_could_not_load_activity)
            StatisticsRecapScreenModel.State.Empty -> RecapMessage(MR.strings.statistics_empty_period)
            is StatisticsRecapScreenModel.State.Success -> {
                val story = current.story
                val palettes = remember(seeds) { mutableMapOf<Int, RecapPalette>() }
                // Pages about no title in particular take the colours of the story's first featured cover.
                val storySeed = story.pages.firstNotNullOfOrNull { page -> page.featured?.let { seeds[it.entryId] } }
                RecapStoryPlayer(
                    story = story,
                    paletteOf = { page ->
                        val seed = page.featured?.let { seeds[it.entryId] } ?: storySeed
                        seed?.let { palettes.getOrPut(it) { RecapPalette.fromSeed(it) } } ?: RecapPalette.Default
                    },
                    title = title,
                    footer = footer,
                    includeNsfw = current.includeNsfw,
                    hiddenTitles = current.hiddenTitles,
                    onIncludeNsfwChange = screenModel::setIncludeNsfw,
                    onHide = screenModel::hide,
                    onShowAgain = screenModel::showAgain,
                    onShare = { image ->
                        try {
                            val file = screenModel.writeImage(image.asAndroidBitmap(), context.cacheImageDir)
                            context.startActivity(file.getUriCompat(context).toShareIntent(context, type = "image/png"))
                        } catch (error: Exception) {
                            logcat(LogPriority.ERROR, error)
                            context.toast(MR.strings.statistics_recap_share_failed)
                        }
                    },
                    onClose = navigator::pop,
                )
            }
        }
    }

    @Composable
    private fun RecapMessage(message: StringResource) {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_recap),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            EmptyScreen(stringRes = message, modifier = Modifier.padding(paddingValues))
        }
    }
}

/** How [period] reads above its story and on its pages. */
@Composable
internal fun recapPeriodTitle(period: StatisticsRecapPeriod): String {
    val formats = rememberRecapFormats()
    return when (period) {
        is StatisticsRecapPeriod.Year -> if (period.isSoFar) {
            stringResource(MR.strings.statistics_recap_year_so_far, period.year.toString())
        } else {
            period.year.toString()
        }
        is StatisticsRecapPeriod.Month -> formats.monthYear(period.month)
        is StatisticsRecapPeriod.Window -> period.label
    }
}
