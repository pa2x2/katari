package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.track.components.TrackLogoIcon
import mihon.entry.interactions.EntryTrackingServiceDescriptor
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    service: EntryTrackingServiceDescriptor,
    isLoggedIn: Boolean,
    displayUsername: String,
    onClick: (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    Box(modifier = Modifier.highlightBackground(highlighted)) {
        Row(
            modifier = modifier
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .fillMaxWidth()
                .padding(horizontal = PrefsHorizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackLogoIcon(name = service.name, logoResource = service.logoResource)
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = service.name,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = TitleFontSize,
                    fontWeight = FontWeight.Medium,
                )
                if (isLoggedIn && displayUsername.isNotBlank()) {
                    Text(
                        text = displayUsername,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isLoggedIn) {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(32.dp),
                    tint = Color(0xFF4CAF50),
                    contentDescription = stringResource(MR.strings.login_success),
                )
            }
        }
    }
}
