package tachiyomi.presentation.core.components.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

internal object ReaderChromeDefaults {
    @Composable
    fun containerColor(): Color {
        val colorScheme = MaterialTheme.colorScheme
        val isDark = colorScheme.surface.luminance() < 0.5f
        return colorScheme
            .surfaceColorAtElevation(3.dp)
            .copy(alpha = if (isDark) 0.9f else 0.95f)
    }
}
