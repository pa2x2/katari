package mihon.presentation.kit.automation

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal actual fun Modifier.exposeTestTags(): Modifier = semantics { testTagsAsResourceId = true }
