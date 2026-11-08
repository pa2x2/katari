package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.graphics.ImageBitmap
import app.katari.video.engine.spi.player.SubtitleImage

/** A picture subtitle's pixels as a bitmap to draw. */
internal expect fun SubtitleImage.toImageBitmap(): ImageBitmap
