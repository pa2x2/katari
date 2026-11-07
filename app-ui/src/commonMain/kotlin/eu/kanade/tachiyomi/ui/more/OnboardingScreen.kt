package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.sync.ui.signin.SyncJoinOrigin
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.onboarding.OnboardingScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.more.settings.screen.data.rememberBackupFilePicker
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*
import tachiyomi.presentation.core.util.collectAsState

class OnboardingScreen : Screen(), FullWindowScreen, SyncJoinOrigin {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow

        val onboardingPreferences = remember(graph) { graph.onboardingPreferences }
        val shownOnboardingFlow by onboardingPreferences.shownOnboardingFlow.collectAsState()
        val migrationPromptHandled by onboardingPreferences.mihonMigrationPromptHandled.collectAsState()
        val mihonApp = remember(graph) { graph.mihonApp }
        val installedMihon = remember(mihonApp) { mihonApp.findInstalled() }
        val showMihonMigration = !shownOnboardingFlow && !migrationPromptHandled && installedMihon != null

        val finishOnboarding: () -> Unit = {
            onboardingPreferences.mihonMigrationPromptHandled.set(true)
            onboardingPreferences.shownOnboardingFlow.set(true)
            navigator.pop()
        }

        // The restore runs in the background while the guide goes on, so its screen opens over the guide.
        val chooseMihonBackup = rememberBackupFilePicker(mimeType = "application/*") { uri ->
            if (uri == null) return@rememberBackupFilePicker
            navigator.push(RestoreBackupScreen(uri))
        }

        // Prevent exiting if onboarding hasn't been completed
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = !shownOnboardingFlow,
            onBackCompleted = {},
        )

        OnboardingScreen(
            onComplete = finishOnboarding,
            offersMihon = showMihonMigration,
            onOpenMihon = {
                if (installedMihon == null || !mihonApp.open(context, installedMihon)) {
                    messages.show(MR.strings.app_not_available)
                }
            },
            onChooseMihonBackup = chooseMihonBackup,
        )
    }
}
