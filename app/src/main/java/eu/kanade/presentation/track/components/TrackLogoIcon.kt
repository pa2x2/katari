package eu.kanade.presentation.track.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import mihon.presentation.kit.content.LogoFrame
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.theme.KatariPreviewTheme
import tachiyomi.presentation.core.util.clickableNoIndication

@Composable
fun TrackLogoIcon(
    name: String,
    logoResource: Int,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick != null) {
        Modifier.clickableNoIndication(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
    }

    LogoFrame(size = LogoSize.Large, modifier = modifier) {
        Image(
            painter = painterResource(logoResource),
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackLogoIconPreviews(
    @PreviewParameter(TrackLogoIconPreviewProvider::class)
    data: TrackLogoIconPreviewData,
) {
    KatariPreviewTheme {
        TrackLogoIcon(
            name = data.name,
            logoResource = data.logoResource,
            onClick = null,
            onLongClick = null,
        )
    }
}
