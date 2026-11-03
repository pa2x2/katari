package eu.kanade.presentation.entry.components.preview

import androidx.compose.runtime.Composable
import mihon.core.common.image.progressive.ProgressiveImageState

// Desktop decodes no image progressively, so a page shows once it has loaded.
@Composable
internal actual fun rememberProgressivePreview(state: ProgressiveImageState?): ProgressivePreview =
    ProgressivePreview(image = null, keepsAnimation = false)
