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
import app.katari.telemetry.Telemetry
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The first-run guide: how the person starts (new, from a Katari backup, or from another app's), that start's own page,
 * then appearance, storage, permissions, linking to a sync account, and the last page.
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val graph = LocalAppUiGraph.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var start by rememberSaveable { mutableStateOf(OnboardingStart.New) }
    val steps = remember(graph, start) {
        val hasPermissionStep = graph.onboardingPermissions.isOffered || Telemetry.isIncluded
        listOfNotNull(
            WelcomeStep(start = start, onStartChange = { start = it }),
            when (start) {
                OnboardingStart.New -> null
                OnboardingStart.Backup -> RestoreStep()
                OnboardingStart.OtherApp -> OtherAppStep()
            },
            ThemeStep(graph.uiPreferences, graph.appThemeHost),
            StorageStep(graph.storagePreferences),
            PermissionStep(
                permissions = graph.onboardingPermissions,
                privacyPreferences = graph.privacyPreferences,
                telemetryIncluded = Telemetry.isIncluded,
            ).takeIf { hasPermissionStep },
            SyncStep(graph.syncStatusState),
            FinishStep(startedFromBackup = start != OnboardingStart.New),
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
        RestoreStatusLine()
        steps[it].Content()
    }
}
