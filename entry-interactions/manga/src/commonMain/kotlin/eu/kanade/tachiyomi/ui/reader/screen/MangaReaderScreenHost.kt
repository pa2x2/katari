package eu.kanade.tachiyomi.ui.reader.screen

import androidx.compose.runtime.Composable
import eu.kanade.presentation.reader.appbars.ReaderMenuBounds
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry

/**
 * What the reader screen gets from the platform that hosts it: what the platform does its own way, and the actions
 * that reach outside the screen.
 */
internal class MangaReaderScreenHost(
    /** The window that saves, shares and copies page images. */
    val context: PlatformContext,
    val sharedSettingsRegistry: ReaderSharedSettingsRegistry,
    /** What the chapter's web menu offers, which leaves out what the platform can't do, such as sharing on desktop. */
    val webViewActions: List<EntryChildWebViewAction>,
    /** Whether the window has a display cutout that pages can be drawn under. */
    val hasDisplayCutout: @Composable () -> Boolean,
    /** Leaves the reader from its back arrow. */
    val onNavigateUp: () -> Unit,
    /** Closes the reader when its chapter couldn't open. */
    val onClose: () -> Unit,
    val onOpenEntry: () -> Unit,
    val onOpenWebView: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    /** The reader's own settings screen, from the settings sheet's menu. */
    val onOpenDefaultSettings: () -> Unit,
    /**
     * The text features' controls, translations and sheets over the pages, given whether the reader's controls show
     * and where they are; null where the platform can't recognize text, which leaves out their button too.
     */
    val textLayer: (@Composable (menuVisible: Boolean, menuBounds: ReaderMenuBounds) -> Unit)?,
)
