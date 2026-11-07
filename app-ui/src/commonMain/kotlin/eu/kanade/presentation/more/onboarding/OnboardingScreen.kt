package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import mihon.app.ui.di.LocalAppUiGraph
import mihon.presentation.kit.feedback.GuideScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
    showMihonMigration: Boolean,
    onOpenMihon: () -> Unit,
    onMigrateFromMihon: () -> Unit,
) {
    val graph = LocalAppUiGraph.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember(graph, showMihonMigration, onRestoreBackup, onOpenMihon, onMigrateFromMihon) {
        val hasPermissionStep = graph.onboardingPermissions.isOffered || graph.appBuild.telemetryIncluded
        listOfNotNull(
            ThemeStep(graph.uiPreferences, graph.appThemeHost),
            StorageStep(graph.storagePreferences, migratingFromMihon = showMihonMigration),
            PermissionStep(
                permissions = graph.onboardingPermissions,
                privacyPreferences = graph.privacyPreferences,
                telemetryIncluded = graph.appBuild.telemetryIncluded,
            ).takeIf { hasPermissionStep },
            GuidesStep(
                onRestoreBackup = onRestoreBackup,
                showMihonMigration = showMihonMigration,
                onOpenMihon = onOpenMihon,
                onMigrateFromMihon = onMigrateFromMihon,
            ),
        )
    }
    val isLastStep = currentStep == steps.lastIndex
    val goNext: () -> Unit = {
        if (isLastStep) {
            onComplete()
        } else {
            currentStep++
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = currentStep != 0,
        onBackCompleted = { currentStep-- },
    )

    GuideScreen(
        page = currentStep,
        pageCount = steps.size,
        heading = { steps[it].heading() },
        nextText = stringResource(
            if (isLastStep) {
                MR.strings.onboarding_action_finish
            } else {
                MR.strings.onboarding_action_next
            },
        ),
        onNextClick = goNext,
        canGoNext = steps[currentStep].isComplete,
        skipText = stringResource(MR.strings.onboarding_action_skip).takeIf { steps[currentStep].canSkip },
        onSkipClick = goNext.takeIf { steps[currentStep].canSkip },
    ) {
        steps[it].Content()
    }
}
