package eu.kanade.presentation.reader.appbars

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.chrome.ui.ReaderChromeBottomBar
import mihon.entry.viewer.chrome.ui.ReaderChromeBottomBarAction
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.CropOff
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ViewList
import mihon.icons.materialsymbols.rounded.Crop
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Translate
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
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
            icon = readingMode.icon,
            contentDescription = stringResource(MR.strings.viewer),
            onClick = onClickReadingMode,
            modifier = Modifier.semantics { stateDescription = readingModeDescription },
        )

        val orientationDescription = stringResource(orientation.stringRes)
        ReaderChromeBottomBarAction(
            icon = orientation.icon,
            contentDescription = stringResource(MR.strings.rotation_type),
            onClick = onClickOrientation,
            modifier = Modifier.semantics { stateDescription = orientationDescription },
        )

        ReaderChromeBottomBarAction(
            icon = if (cropEnabled) MaterialSymbols.Rounded.Crop else CustomMaterialSymbols.Rounded.CropOff,
            contentDescription = stringResource(MR.strings.pref_crop_borders),
            onClick = onClickCropBorder,
        )

        if (showAutoScrollToggle) {
            ReaderChromeBottomBarAction(
                icon = if (autoScrollActive) MaterialSymbols.RoundedFilled.Pause else MaterialSymbols.Rounded.PlayArrow,
                contentDescription = stringResource(MR.strings.pref_auto_scroll),
                onClick = onClickAutoScroll,
            )
        }

        ReaderChromeBottomBarAction(
            icon = MaterialSymbols.AutoMirroredRounded.ViewList,
            contentDescription = stringResource(MR.strings.reader_table_of_contents),
            onClick = onClickChapterNavigation,
        )

        val textTranslationState = stringResource(if (textTranslationActive) MR.strings.on else MR.strings.off)
        ReaderChromeBottomBarAction(
            icon = MaterialSymbols.Rounded.Translate,
            contentDescription = stringResource(MR.strings.reader_text_translate),
            onClick = onClickTextTranslation,
            modifier = Modifier.semantics { stateDescription = textTranslationState },
            tone = if (textTranslationActive) ContentTone.Primary else ContentTone.Inherit,
        )

        ReaderChromeBottomBarAction(
            icon = MaterialSymbols.Rounded.Settings,
            contentDescription = stringResource(MR.strings.action_settings),
            onClick = onClickSettings,
        )
    }
}
