package tachiyomi.presentation.core.i18n

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun fileSize(bytes: Long): String {
    val context = LocalContext.current
    return remember(context, bytes) { Formatter.formatFileSize(context, bytes) }
}

@Composable
actual fun shortFileSize(bytes: Long): String {
    val context = LocalContext.current
    return remember(context, bytes) { Formatter.formatShortFileSize(context, bytes) }
}
