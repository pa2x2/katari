package eu.kanade.tachiyomi.ui.more

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.presentation.more.settings.screen.SettingsTrackingScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.stats.recap.delivery.subscribeUnopenedYearRecap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.ProfilePickerScreen
import mihon.feature.profiles.ui.handleProfileShortcut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_more)
            return TabOptions(
                index = 4u,
                title = stringResource(MR.strings.label_more),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SettingsScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val viewModel = viewModel { MoreViewModel() }
        val profileManager = remember { Injekt.get<ProfileManager>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val downloadQueueState by viewModel.downloadQueueState.collectAsState()
        val hasNewRecap by viewModel.hasNewRecap.collectAsState()
        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = viewModel.downloadedOnly,
            onDownloadedOnlyChange = { viewModel.downloadedOnly = it },
            incognitoMode = viewModel.incognitoMode,
            onIncognitoModeChange = { viewModel.incognitoMode = it },
            onClickDownloadQueue = { navigator.push(DownloadQueueScreen) },
            onClickTracking = { navigator.push(SettingsTrackingScreen) },
            onClickCategories = { navigator.push(CategoryScreen()) },
            onClickStats = { navigator.push(StatsScreen()) },
            hasNewRecap = hasNewRecap,
            onClickDataAndStorage = { navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage)) },
            onClickProfiles = {
                scope.launch {
                    handleProfileShortcut(
                        context = context,
                        profileManager = profileManager,
                        uiPreferences = uiPreferences,
                        onOpenProfilePicker = { navigator.push(ProfilePickerScreen()) },
                        onBeforeSwitch = { navigator.popUntilRoot() },
                    )
                }
            },
            onClickSettings = { navigator.push(SettingsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
        )
    }
}

private class MoreViewModel(
    downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) : ViewModel() {

    var downloadedOnly by libraryPreferences.downloadedOnly.asState(viewModelScope)
    var incognitoMode by basePreferences.incognitoMode.asState(viewModelScope)

    val downloadQueueState: StateFlow<DownloadQueueState> = downloadRuntime.state
        .map { runtime ->
            val queueSize = runtime.queue.sumOf { it.items.size }
            when {
                queueSize == 0 -> DownloadQueueState.Stopped
                !runtime.isRunning -> DownloadQueueState.Paused(queueSize)
                else -> DownloadQueueState.Downloading(queueSize)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DownloadQueueState.Stopped,
        )

    val hasNewRecap: StateFlow<Boolean> = subscribeUnopenedYearRecap()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
