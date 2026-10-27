package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
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

/**
 * The reader's actions. [shareWidth] spreads them across a bar spanning the window, as in a phone-sized window;
 * otherwise each keeps its own touch target, for the floating row of a wider one.
 */
@Composable
fun RowScope.ReaderBottomBarActions(
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
    shareWidth: Boolean,
) {
    val readingModeDescription = stringResource(readingMode.stringRes)
    ReaderChromeBottomBarAction(
        icon = readingMode.icon,
        contentDescription = stringResource(MR.strings.viewer),
        onClick = onClickReadingMode,
        modifier = Modifier.semantics { stateDescription = readingModeDescription },
        shareWidth = shareWidth,
    )

    val orientationDescription = stringResource(orientation.stringRes)
    ReaderChromeBottomBarAction(
        icon = orientation.icon,
        contentDescription = stringResource(MR.strings.rotation_type),
        onClick = onClickOrientation,
        modifier = Modifier.semantics { stateDescription = orientationDescription },
        shareWidth = shareWidth,
    )

    ReaderChromeBottomBarAction(
        icon = if (cropEnabled) MaterialSymbols.Rounded.Crop else CustomMaterialSymbols.Rounded.CropOff,
        contentDescription = stringResource(MR.strings.pref_crop_borders),
        onClick = onClickCropBorder,
        shareWidth = shareWidth,
    )

    if (showAutoScrollToggle) {
        ReaderChromeBottomBarAction(
            icon = if (autoScrollActive) MaterialSymbols.RoundedFilled.Pause else MaterialSymbols.Rounded.PlayArrow,
            contentDescription = stringResource(MR.strings.pref_auto_scroll),
            onClick = onClickAutoScroll,
            shareWidth = shareWidth,
        )
    }

    ReaderChromeBottomBarAction(
        icon = MaterialSymbols.AutoMirroredRounded.ViewList,
        contentDescription = stringResource(MR.strings.reader_table_of_contents),
        onClick = onClickChapterNavigation,
        shareWidth = shareWidth,
    )

    val textTranslationState = stringResource(if (textTranslationActive) MR.strings.on else MR.strings.off)
    ReaderChromeBottomBarAction(
        icon = MaterialSymbols.Rounded.Translate,
        contentDescription = stringResource(MR.strings.reader_text_translate),
        onClick = onClickTextTranslation,
        modifier = Modifier.semantics { stateDescription = textTranslationState },
        tone = if (textTranslationActive) ContentTone.Primary else ContentTone.Inherit,
        shareWidth = shareWidth,
    )

    ReaderChromeBottomBarAction(
        icon = MaterialSymbols.Rounded.Settings,
        contentDescription = stringResource(MR.strings.action_settings),
        onClick = onClickSettings,
        shareWidth = shareWidth,
    )
}
