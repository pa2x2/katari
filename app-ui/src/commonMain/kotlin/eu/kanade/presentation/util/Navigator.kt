package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import mihon.presentation.kit.feedback.ScreenMessagesHost
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

interface Tab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}
}

abstract class Screen : Screen {

    override val key: ScreenKey = uniqueScreenKey
}

interface AssistContentScreen {
    fun onProvideAssistUrl(): String?
}

interface HandlesOwnBackPress

@Composable
fun DefaultNavigatorScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val slideDistance = rememberSlideDistance()
    ScreenTransition(
        navigator = navigator,
        transition = {
            materialSharedAxisX(
                forward = navigator.lastEvent != StackEvent.Pop,
                slideDistance = slideDistance,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun ScreenTransition(
    navigator: Navigator,
    transition: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() },
) {
    // The most recently registered back handler wins, and handlers composed in one pass register in composition
    // order. Composed ahead of the screens, popping stays below their own handlers even when the navigator first
    // composes with a screen already on top, as desktop onboarding does; otherwise back skipped onboarding's guard.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = navigator.canPop && navigator.lastItem !is HandlesOwnBackPress,
        onBackCompleted = navigator::pop,
    )

    val lasting = navigator.items.lastOrNull { it is StaysComposedUnderCover }
    if (lasting == null) {
        AnimatedContent(
            targetState = navigator.lastItem,
            transitionSpec = transition,
            modifier = modifier,
            label = "transition",
        ) { screen ->
            navigator.saveableState("transition", screen) {
                ScreenMessagesHost { content(screen) }
            }
        }
        return
    }
    // The lasting screen keeps one place under the transition, so moving it in and out of the transition doesn't
    // compose it anew; the transition shows nothing while it is on top.
    Box(modifier = modifier) {
        key(lasting.key) {
            CoverableScreen(covered = navigator.lastItem !== lasting) {
                // Shown as it is, outside the transition, which gives the content its scope otherwise
                AnimatedVisibility(visible = true, enter = EnterTransition.None, exit = ExitTransition.None) {
                    navigator.saveableState("lasting", lasting) {
                        ScreenMessagesHost { content(lasting) }
                    }
                }
            }
        }
        AnimatedContent(
            targetState = navigator.lastItem,
            transitionSpec = transition,
            modifier = Modifier.fillMaxSize(),
            label = "transition",
        ) { screen ->
            if (screen === lasting) {
                Spacer(modifier = Modifier.fillMaxSize())
            } else {
                navigator.saveableState("transition", screen) {
                    ScreenMessagesHost { content(screen) }
                }
            }
        }
    }
}
