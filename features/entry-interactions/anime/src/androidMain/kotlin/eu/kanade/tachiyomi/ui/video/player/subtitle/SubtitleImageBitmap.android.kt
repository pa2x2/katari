package eu.kanade.tachiyomi.ui.video.player.subtitle

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.katari.video.engine.spi.player.SubtitleImage

internal actual fun SubtitleImage.toImageBitmap(): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
