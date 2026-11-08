package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.presentation.kit.feedback.GuideScreen
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The first-run guide: how the person starts (new, from a backup, or from Mihon where [offersMihon]), that start's own
 * page, then appearance, storage, permissions, linking to a sync account, and the last page.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    offersMihon: Boolean,
    onOpenMihon: () -> Unit,
    onChooseMihonBackup: () -> Unit,
) {
    val graph = LocalAppUiGraph.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var start by rememberSaveable { mutableStateOf(OnboardingStart.New) }
    val steps = remember(graph, start, offersMihon, onOpenMihon, onChooseMihonBackup) {
        val hasPermissionStep = graph.onboardingPermissions.isOffered || graph.appBuild.telemetryIncluded
        listOfNotNull(
            WelcomeStep(start = start, onStartChange = { start = it }, offersMihon = offersMihon),
            when (start) {
                OnboardingStart.New -> null
                OnboardingStart.Backup -> RestoreStep()
                OnboardingStart.Mihon -> MihonStep(onOpenMihon, onChooseMihonBackup)
            },
            ThemeStep(graph.uiPreferences, graph.appThemeHost),
            StorageStep(graph.storagePreferences, migratingFromMihon = start == OnboardingStart.Mihon),
            PermissionStep(
                permissions = graph.onboardingPermissions,
                privacyPreferences = graph.privacyPreferences,
                telemetryIncluded = graph.appBuild.telemetryIncluded,
            ).takeIf { hasPermissionStep },
            SyncStep(graph.syncStatusState),
            FinishStep(),
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
