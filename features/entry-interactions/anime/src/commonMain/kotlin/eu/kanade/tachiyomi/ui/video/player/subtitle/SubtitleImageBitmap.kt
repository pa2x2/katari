package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.graphics.ImageBitmap
import mihon.video.engine.spi.player.SubtitleImage

/** A picture subtitle's pixels as a bitmap to draw. */
internal expect fun SubtitleImage.toImageBitmap(): ImageBitmap
