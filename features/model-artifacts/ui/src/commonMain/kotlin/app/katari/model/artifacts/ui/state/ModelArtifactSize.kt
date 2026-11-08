package app.katari.model.artifacts.ui.state

import androidx.compose.runtime.Composable
import tachiyomi.presentation.core.i18n.shortFileSize

/** [bytes] as a short size for the user, such as "12 MB", the way every model size is shown. */
@Composable
fun formatModelArtifactSize(bytes: Long): String = shortFileSize(bytes)
