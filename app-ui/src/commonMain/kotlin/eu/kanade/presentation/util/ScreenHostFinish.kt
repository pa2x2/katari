package eu.kanade.presentation.util

import androidx.compose.runtime.Composable

/**
 * Closes what hosts the composed screens, for a screen opened on its own that has no screen beneath it to go back to.
 */
@Composable
expect fun rememberScreenHostFinish(): () -> Unit
