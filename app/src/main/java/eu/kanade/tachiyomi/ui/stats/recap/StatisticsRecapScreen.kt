package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.story.RecapStoryPlayer
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.StoryPalette
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A recap of [period]: a story for years and months, the summary card alone for any other window. */
data class StatisticsRecapScreen(private val period: StatisticsRecapPeriod) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = viewModel { StatisticsRecapViewModel(period) }
        val state by viewModel.state.collectAsState()
        val seeds by viewModel.coverSeeds.collectAsState()
        val title = recapPeriodTitle(period)
        val footer = "${stringResource(MR.strings.app_name)} · $title"

        when (val current = state) {
            StatisticsRecapViewModel.State.Loading -> LoadingScreen()
            StatisticsRecapViewModel.State.Failed -> RecapMessage(MR.strings.statistics_could_not_load_activity)
            StatisticsRecapViewModel.State.Empty -> RecapMessage(MR.strings.statistics_empty_period)
            is StatisticsRecapViewModel.State.Success -> {
                val story = current.story
                val palettes = remember(seeds) { mutableMapOf<Int, StoryPalette>() }
                val themePalette = StoryPalette.App
                // A page about a title takes its cover's colours, and one about no title in particular the first
                // usable cover's; without a usable colour, and until covers load, pages take the app theme's.
                val storySeed = story.pages.firstNotNullOfOrNull { page -> page.featured?.let { seeds[it.entryId] } }
                RecapStoryPlayer(
                    story = story,
                    paletteOf = { page ->
                        val featured = page.featured
                        val seed = if (featured != null) seeds[featured.entryId] else storySeed
                        seed?.let { palettes.getOrPut(it) { StoryPalette.fromSeed(it) } } ?: themePalette
                    },
                    title = title,
                    footer = footer,
                    includeNsfw = current.includeNsfw,
                    hiddenTitles = current.hiddenTitles,
                    onIncludeNsfwChange = viewModel::setIncludeNsfw,
                    onHide = viewModel::hide,
                    onShowAgain = viewModel::showAgain,
                    onShare = { image ->
                        try {
                            val file = viewModel.writeImage(image.asAndroidBitmap(), context.cacheImageDir)
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
