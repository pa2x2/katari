package mihon.core.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

actual typealias PlatformContext = Context

@Composable
actual fun currentPlatformContext(): PlatformContext = LocalContext.current
