package mihon.video.engine.spi.media3

import android.net.Uri
import androidx.media3.common.MediaMetadata
import mihon.video.engine.spi.VideoMetadata

fun VideoMetadata.toMediaMetadata(): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(series)
    .setArtworkUri(artworkUrl?.let(Uri::parse))
    .build()
