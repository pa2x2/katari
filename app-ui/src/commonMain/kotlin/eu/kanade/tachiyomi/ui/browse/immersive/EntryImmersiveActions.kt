package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.entry.interactions.media.EntryImmersiveMediaSwitch
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.icons.materialsymbols.rounded.Fullscreen
import app.katari.icons.materialsymbols.rounded.FullscreenExit
import app.katari.icons.materialsymbols.rounded.Info
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconToggleButton
import app.katari.presentation.kit.refresh.RefreshButton
import app.katari.presentation.kit.window.WindowFullScreen
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What the immersive view itself offers, whichever title it shows: refreshing, the top, full screen and leaving, and
 * the switches the shown media adds, such as a clip's subtitles.
 */
internal class ImmersiveViewActions(
    val onRefresh: () -> Unit,
    val refreshEnabled: Boolean,
    /** Null on the first title, where there is no top to move to. */
    val onBackToTop: (() -> Unit)?,
    val fullScreen: WindowFullScreen?,
    val onExit: () -> Unit,
    val mediaSwitches: List<EntryImmersiveMediaSwitch>,
)

/**
 * What can be done with the title shown: opening its episode or chapter (null while it can't be), adding it to the
 * library or removing it, and its details.
 */
internal class ImmersiveEntryActions(
    val entry: Entry,
    val chapterName: String?,
    val onOpenChapter: (() -> Unit)?,
    val onLibraryAction: () -> Unit,
    val onDetails: () -> Unit,
)

/** The immersive view's own actions as icon buttons, in the order the bar and the side panel show them. */
@Composable
internal fun ImmersiveViewActionButtons(actions: ImmersiveViewActions) {
    RefreshButton(onClick = actions.onRefresh, modifier = Modifier.mediaControl(), enabled = actions.refreshEnabled)
    actions.onBackToTop?.let { onBackToTop ->
        ImmersiveActionButton(
            label = stringResource(MR.strings.action_move_to_top),
            icon = MaterialSymbols.Rounded.KeyboardArrowUp,
            onClick = onBackToTop,
        )
    }
    actions.fullScreen?.let { fullScreen ->
        ImmersiveActionButton(
            label = stringResource(
                if (fullScreen.isFullScreen) {
                    MR.strings.action_exit_full_screen
                } else {
                    MR.strings.action_enter_full_screen
                },
            ),
            icon = if (fullScreen.isFullScreen) {
                MaterialSymbols.Rounded.FullscreenExit
            } else {
                MaterialSymbols.Rounded.Fullscreen
            },
            onClick = fullScreen::toggle,
        )
    }
    ImmersiveActionButton(
        label = stringResource(MR.strings.browse_exit_immersive),
        icon = MaterialSymbols.Rounded.Close,
        onClick = actions.onExit,
    )
    actions.mediaSwitches.forEach { switch ->
        IconToggleButton(
            icon = switch.icon,
            contentDescription = switch.label,
            checked = switch.on,
            onCheckedChange = { switch.onToggle() },
            modifier = Modifier.mediaControl(),
        )
    }
}

/** The title's actions as icon buttons, for the bar over the media. */
@Composable
internal fun ImmersiveEntryActionButtons(actions: ImmersiveEntryActions) {
    ImmersiveActionButton(
        label = libraryActionLabel(actions.entry),
        icon = EntryActionIcons.library(actions.entry.favorite),
        onClick = actions.onLibraryAction,
    )
    ImmersiveActionButton(
        label = stringResource(MR.strings.action_details),
        icon = MaterialSymbols.Rounded.Info,
        onClick = actions.onDetails,
    )
    val presentation = actions.entry.type.entryTypePresentation()
    ImmersiveActionButton(
        label = stringResource(presentation.immersiveOpenLabel),
        icon = presentation.immersiveOpenIcon,
        onClick = { actions.onOpenChapter?.invoke() },
        enabled = actions.onOpenChapter != null,
    )
}

@Composable
internal fun libraryActionLabel(entry: Entry): String =
    stringResource(if (entry.favorite) MR.strings.remove_from_library else MR.strings.add_to_library)

// The scrim or panel behind these buttons gives them their contrast, so they are bare icons in the media content colour.
@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        modifier = Modifier.mediaControl(),
        enabled = enabled,
    )
}
