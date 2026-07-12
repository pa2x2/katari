package eu.kanade.presentation.reader.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tachiyomi.presentation.core.components.AdaptiveSheet as AdaptiveSheetImpl

@Composable
fun AdaptiveSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableImplicitDismiss: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isTabletUi = isReaderTabletUi()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        AdaptiveSheetImpl(
            isTabletUi = isTabletUi,
            enableImplicitDismiss = enableImplicitDismiss,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
@ReadOnlyComposable
fun isReaderTabletUi(): Boolean {
    return LocalConfiguration.current.smallestScreenWidthDp >= 720
}

private val dialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)
