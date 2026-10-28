package eu.kanade.presentation.entry.components.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import mihon.core.common.image.progressive.ProgressiveImageState

/**
 * What a preview tile shows of a page that arrives progressively: the frame to draw so far, or null, and whether a
 * finished replayable animation keeps playing instead of the decoded page.
 */
internal class ProgressivePreview(val image: ImageBitmap?, val keepsAnimation: Boolean)

@Composable
internal expect fun rememberProgressivePreview(state: ProgressiveImageState?): ProgressivePreview
