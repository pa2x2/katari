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
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryDownloadRuntimeFeature
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.ProfilePickerScreen
import mihon.feature.profiles.ui.handleProfileShortcut
import mihon.feature.support.SupportUsScreen
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object MoreTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_more_enter)
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
        val screenModel = rememberScreenModel { MoreScreenModel() }
        val profileManager = remember { Injekt.get<ProfileManager>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val downloadQueueState by screenModel.downloadQueueState.collectAsState()
        MoreScreen(
            downloadQueueStateProvider = { downloadQueueState },
            downloadedOnly = screenModel.downloadedOnly,
            onDownloadedOnlyChange = { screenModel.downloadedOnly = it },
            incognitoMode = screenModel.incognitoMode,
            onIncognitoModeChange = { screenModel.incognitoMode = it },
            onClickDownloadQueue = { navigator.push(DownloadQueueScreen) },
            onClickTracking = { navigator.push(SettingsTrackingScreen) },
            onClickCategories = { navigator.push(CategoryScreen()) },
            onClickStats = { navigator.push(StatsScreen()) },
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
            onClickSupport = { navigator.push(SupportUsScreen()) },
            onClickAbout = { navigator.push(SettingsScreen(SettingsScreen.Destination.About)) },
        )
    }
}

private class MoreScreenModel(
    downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    basePreferences: BasePreferences = Injekt.get(),
    libraryPreferences: LibraryPreferences = Injekt.get(),
) : ScreenModel {

    var downloadedOnly by libraryPreferences.downloadedOnly.asState(screenModelScope)
    var incognitoMode by basePreferences.incognitoMode.asState(screenModelScope)

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
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DownloadQueueState.Stopped,
        )
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
