package eu.kanade.presentation.reader.appbars

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.entry.interactions.manga.R
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBar
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBarAction
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderBottomBar(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    showAutoScrollToggle: Boolean,
    autoScrollActive: Boolean,
    onClickAutoScroll: () -> Unit,
    onClickSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderChromeBottomBar(modifier = modifier) {
        val readingModeDescription = stringResource(readingMode.stringRes)
        ReaderChromeBottomBarAction(
            onClick = onClickReadingMode,
            modifier = Modifier.semantics { stateDescription = readingModeDescription },
        ) {
            Icon(
                painter = painterResource(readingMode.iconRes),
                contentDescription = stringResource(MR.strings.viewer),
            )
        }

        val orientationDescription = stringResource(orientation.stringRes)
        ReaderChromeBottomBarAction(
            onClick = onClickOrientation,
            modifier = Modifier.semantics { stateDescription = orientationDescription },
        ) {
            Icon(
                imageVector = orientation.icon,
                contentDescription = stringResource(MR.strings.rotation_type),
            )
        }

        ReaderChromeBottomBarAction(onClick = onClickCropBorder) {
            Icon(
                painter = painterResource(if (cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp),
                contentDescription = stringResource(MR.strings.pref_crop_borders),
            )
        }

        if (showAutoScrollToggle) {
            ReaderChromeBottomBarAction(onClick = onClickAutoScroll) {
                Icon(
                    imageVector = if (autoScrollActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(MR.strings.pref_auto_scroll),
                )
            }
        }

        ReaderChromeBottomBarAction(onClick = onClickSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}
