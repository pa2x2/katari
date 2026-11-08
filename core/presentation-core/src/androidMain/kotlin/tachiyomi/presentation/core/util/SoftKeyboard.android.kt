package tachiyomi.presentation.core.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@Composable
internal actual fun isSoftKeyboardVisible(): Boolean = WindowInsets.isImeVisible
