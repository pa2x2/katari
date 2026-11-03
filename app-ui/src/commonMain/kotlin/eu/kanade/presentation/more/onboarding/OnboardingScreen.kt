package eu.kanade.presentation.more.onboarding

import androidx.compose.animation.AnimatedContent
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
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.presentation.kit.feedback.InfoScreen
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
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
    val slideDistance = rememberSlideDistance()

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

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = currentStep != 0,
        onBackCompleted = { currentStep-- },
    )

    InfoScreen(
        icon = MaterialSymbols.Rounded.RocketLaunch,
        headingText = stringResource(MR.strings.onboarding_heading),
        subtitleText = stringResource(MR.strings.onboarding_description),
        acceptText = stringResource(
            if (isLastStep) {
                MR.strings.onboarding_action_finish
            } else {
                MR.strings.onboarding_action_next
            },
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                materialSharedAxisX(
                    forward = targetState > initialState,
                    slideDistance = slideDistance,
                )
            },
            label = "stepContent",
        ) {
            steps[it].Content()
        }
    }
}
