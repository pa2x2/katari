package mihon.model.artifacts.ui.state

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun formatModelArtifactSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)
