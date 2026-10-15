package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.presentation.kit.feedback.InfoScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
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

    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember(showMihonMigration, onRestoreBackup, onOpenMihon, onMigrateFromMihon) {
        val graph = context.appGraph
        listOf(
            ThemeStep(graph.uiPreferences),
            StorageStep(graph.storagePreferences, migratingFromMihon = showMihonMigration),
            PermissionStep(graph.privacyPreferences),
            GuidesStep(
                onRestoreBackup = onRestoreBackup,
                showMihonMigration = showMihonMigration,
                onOpenMihon = onOpenMihon,
                onMigrateFromMihon = onMigrateFromMihon,
            ),
        )
    }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0) {
        currentStep--
    }

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
        Surface(
            modifier = Modifier
                .padding(vertical = KatariTheme.spacing.s)
                .fillMaxSize(),
            container = SurfaceContainer.Highest,
            shape = KatariTheme.shapes.small,
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
}
