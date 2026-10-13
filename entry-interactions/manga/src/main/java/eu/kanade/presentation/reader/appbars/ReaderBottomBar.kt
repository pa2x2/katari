package eu.kanade.presentation.reader.appbars

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.CropOff
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ViewList
import mihon.icons.materialsymbols.rounded.Crop
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Translate
import mihon.icons.materialsymbols.roundedfilled.Pause
import tachiyomi.i18n.*
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
    onClickChapterNavigation: () -> Unit,
    textTranslationActive: Boolean,
    onClickTextTranslation: () -> Unit,
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
                imageVector = readingMode.icon,
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
                imageVector = if (cropEnabled) MaterialSymbols.Rounded.Crop else CustomMaterialSymbols.Rounded.CropOff,
                contentDescription = stringResource(MR.strings.pref_crop_borders),
            )
        }

        if (showAutoScrollToggle) {
            ReaderChromeBottomBarAction(onClick = onClickAutoScroll) {
                Icon(
                    imageVector = if (autoScrollActive) {
                        MaterialSymbols.RoundedFilled.Pause
                    } else {
                        MaterialSymbols.Rounded.PlayArrow
                    },
                    contentDescription = stringResource(MR.strings.pref_auto_scroll),
                )
            }
        }

        ReaderChromeBottomBarAction(onClick = onClickChapterNavigation) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.ViewList,
                contentDescription = stringResource(MR.strings.reader_table_of_contents),
            )
        }

        val textTranslationState = stringResource(if (textTranslationActive) MR.strings.on else MR.strings.off)
        ReaderChromeBottomBarAction(
            onClick = onClickTextTranslation,
            modifier = Modifier.semantics { stateDescription = textTranslationState },
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Translate,
                contentDescription = stringResource(MR.strings.reader_text_translate),
                tint = if (textTranslationActive) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }

        ReaderChromeBottomBarAction(onClick = onClickSettings) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}
