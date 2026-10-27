package mihon.model.artifacts.ui.state

import androidx.compose.runtime.Composable

/** [bytes] as a short size for the user, such as "12 MB". */
@Composable
expect fun formatModelArtifactSize(bytes: Long): String
