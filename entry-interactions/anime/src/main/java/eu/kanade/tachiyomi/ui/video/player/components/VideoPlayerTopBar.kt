package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun VideoPlayerTopBar(
    videoTitle: String,
    episodeName: String,
    onBack: () -> Unit,
    showPictureInPictureButton: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.76f), Color.Transparent),
                ),
            )
            .videoPlayerSafeContentPadding(includeTop = true)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 12.dp),
        ) {
            Text(
                text = videoTitle,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = episodeName,
                color = Color.White.copy(alpha = 0.74f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoPlayerUtilityButton(onClick = onToggleLock) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = stringResource(MR.strings.anime_playback_lock_controls),
                    tint = Color.White,
                )
            }
            if (showPictureInPictureButton) {
                VideoPlayerUtilityButton(onClick = onEnterPictureInPicture) {
                    Icon(
                        imageVector = Icons.Outlined.PictureInPictureAlt,
                        contentDescription = stringResource(MR.strings.pref_enable_anime_picture_in_picture),
                        tint = Color.White,
                    )
                }
            }
            VideoPlayerUtilityButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(MR.strings.label_settings),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun VideoPlayerUtilityButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick) {
        content()
    }
}
