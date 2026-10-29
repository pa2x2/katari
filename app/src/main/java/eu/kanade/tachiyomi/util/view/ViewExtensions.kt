@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mihon.app.di.appGraph
import mihon.app.ui.host.AppUiRoot

inline fun ComponentActivity.setComposeContent(
    parent: CompositionContext? = null,
    crossinline content: @Composable () -> Unit,
) {
    setContent(parent) {
        AppUiRoot(appGraph) { TestTagsAsResourceIds { content() } }
    }
}

fun ComposeView.setComposeContent(
    content: @Composable () -> Unit,
) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        AppUiRoot(context.appGraph) { TestTagsAsResourceIds { content() } }
    }
}

/**
 * Lets UI automation outside the app, such as the showcase capture scenes, find test tags as resource ids. It has to be
 * turned on above the tags in each window, and the kit's dropdown menus, popups of their own, turn it on for theirs.
 */
@Composable
@PublishedApi
internal fun TestTagsAsResourceIds(content: @Composable () -> Unit) {
    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }, propagateMinConstraints = true) { content() }
}
