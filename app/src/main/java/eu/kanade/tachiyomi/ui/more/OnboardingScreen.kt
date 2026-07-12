package eu.kanade.tachiyomi.ui.more

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.onboarding.OnboardingScreen
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class OnboardingScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val shownOnboardingFlow by basePreferences.shownOnboardingFlow.collectAsState()
        val migrationPromptHandled by basePreferences.mihonMigrationPromptHandled.collectAsState()
        val mihonPackageName = remember { context.findInstalledMihonPackage() }
        val showMihonMigration = shouldOfferMihonMigration(
            onboardingComplete = shownOnboardingFlow,
            migrationPromptHandled = migrationPromptHandled,
            mihonInstalled = mihonPackageName != null,
        )

        val finishOnboarding: () -> Unit = {
            basePreferences.mihonMigrationPromptHandled.set(true)
            basePreferences.shownOnboardingFlow.set(true)
            navigator.pop()
        }

        val chooseMihonBackup = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            finishOnboarding()
            navigator.push(RestoreBackupScreen(uri.toString()))
        }

        val restoreSettingKey = stringResource(SettingsDataScreen.restorePreferenceKeyString)

        BackHandler(enabled = !shownOnboardingFlow) {
            // Prevent exiting if onboarding hasn't been completed
        }

        OnboardingScreen(
            onComplete = finishOnboarding,
            onRestoreBackup = {
                finishOnboarding()
                SearchableSettings.highlightKey = restoreSettingKey
                navigator.push(SettingsScreen(SettingsScreen.Destination.DataAndStorage))
            },
            showMihonMigration = showMihonMigration,
            onOpenMihon = {
                val intent = mihonPackageName?.let(context.packageManager::getLaunchIntentForPackage)
                if (intent == null) {
                    context.toast(MR.strings.app_not_available)
                } else {
                    try {
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: ActivityNotFoundException) {
                        context.toast(MR.strings.app_not_available)
                    }
                }
            },
            onMigrateFromMihon = {
                chooseMihonBackup.launch("application/*")
            },
        )
    }
}
