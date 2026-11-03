package tachiyomi.presentation.core.util

import androidx.compose.runtime.Composable

/** Whether an on-screen keyboard is showing; always false where the platform has none. */
@Composable
internal expect fun isSoftKeyboardVisible(): Boolean
