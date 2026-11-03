package eu.kanade.presentation.util

import androidx.compose.runtime.Composable

/** Desktop opens no screen on its own: every one is pushed onto a screen it can go back to, so nothing is closed. */
@Composable
actual fun rememberScreenHostFinish(): () -> Unit = {}
