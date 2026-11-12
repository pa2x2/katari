package app.katari.desktop.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import app.katari.entry.interactions.player.AnimePlayerPlacement
import app.katari.presentation.kit.feedback.ScreenMessagesHost
import app.katari.presentation.kit.shortcut.KeyboardShortcutScope
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.first

/**
 * The open visit's player, over the screens of [navigator], the window's navigation: filling the window while its
 * screen is on top, laid out but neither drawn nor given input while a screen it opened covers it, and composed on
 * while it plays in picture-in-picture. The visit ends when its screen leaves the navigation other than for
 * picture-in-picture, as back does, and with the navigation it was opened in, which a profile switch replaces.
 */
@Composable
internal fun WindowAnimePlayer.Host(navigator: Navigator) {
    val visit = visit ?: return
    if (visit.navigator !== navigator) {
        LaunchedEffect(visit) { close(visit) }
        return
    }
    key(visit) {
        // Before the player, so its ViewModels are cleared after the player has let go of them
        DisposableEffect(visit) { onDispose(visit.lifecycle::destroy) }
        LaunchedEffect(visit) {
            snapshotFlow { visit.inPictureInPicture || navigator.items.any { it === visit.screen } }.first { !it }
            close(visit)
        }
        val placement = when {
            visit.inPictureInPicture -> AnimePlayerPlacement.PictureInPicture
            visit.screen != null && navigator.lastItem === visit.screen -> AnimePlayerPlacement.Window
            else -> AnimePlayerPlacement.Covered
        }
        val scope = rememberCoroutineScope()
        val navigation = remember(visit) { WindowPlayerNavigation(this, visit, scope, webViewDestination) }
        Placed(shown = placement == AnimePlayerPlacement.Window) {
            visit.lifecycle.ProvideBeforeScreenContent(provideSaveableState = { _, content -> content() }) {
                player.Content(visit.request, navigation, placement, mainWindowVisibility.isHidden)
            }
        }
    }
}

/** [content] over the window while [shown], and otherwise laid out but neither placed nor given input. */
@Composable
private fun Placed(shown: Boolean, content: @Composable () -> Unit) {
    val backHandlers = rememberNavigationEventDispatcherOwner(enabled = shown)
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides backHandlers) {
        KeyboardShortcutScope(active = shown) {
            // Left unplaced, it draws nothing and no pointer reaches it, while its effects keep running
            Box(
                modifier = Modifier.fillMaxSize().layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { if (shown) placeable.place(0, 0) }
                },
            ) {
                // As a screen of the navigation's transition, which gives screens their scope and messages
                AnimatedVisibility(visible = true, enter = EnterTransition.None, exit = ExitTransition.None) {
                    ScreenMessagesHost(content)
                }
            }
        }
    }
}
