package eu.kanade.presentation.util

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

/** Finishes the activity, which Android opens such a screen in. */
@Composable
actual fun rememberScreenHostFinish(): () -> Unit {
    val activity = LocalActivity.current
    return { activity?.finish() }
}
