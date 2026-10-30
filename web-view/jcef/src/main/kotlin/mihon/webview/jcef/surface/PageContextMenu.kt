package mihon.webview.jcef.surface

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.presentation.kit.menu.ContextMenu
import mihon.presentation.kit.menu.DropdownMenuDivider
import mihon.presentation.kit.menu.DropdownMenuItem
import org.cef.browser.CefBrowser
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefRunContextMenuCallback
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page's right-click menu, as a browser offers it for what was clicked: a link opens in the user's browser or is
 * copied, a text field's text is cut, copied, pasted or selected as far as the field allows, a selection is copied, and
 * anywhere else the page goes back, forward or reloads. Chromium waits for the menu's answer, which closing it gives.
 */
@Composable
internal fun BoxScope.PageContextMenu(
    page: ChromiumPage,
    request: PageMenuRequest,
    linkOpener: LinkOpener,
    clipboard: TextClipboard,
) {
    val context = currentPlatformContext()
    val scale = page.frame.size.scale
    val close = {
        request.dismiss()
        if (page.input.contextMenu === request) page.input.contextMenu = null
    }
    ContextMenu(
        position = Offset((request.x * scale).toFloat(), (request.y * scale).toFloat()),
        onDismissRequest = close,
    ) {
        request.linkUrl?.let { link ->
            DropdownMenuItem(
                label = stringResource(MR.strings.action_open_in_browser),
                onClick = { linkOpener.open(context, link, forceDefaultBrowser = true) },
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_copy_link),
                onClick = { clipboard.copy(link, link) },
            )
        }
        val focusedFrame = { request.browser.focusedFrame ?: request.browser.mainFrame }
        if (request.linkUrl != null && (request.editable || request.hasSelection)) DropdownMenuDivider()
        if (request.editable) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_cut),
                onClick = { focusedFrame().cut() },
                enabled = request.can(CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_CUT),
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_copy),
                onClick = { focusedFrame().copy() },
                enabled = request.can(CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_COPY),
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_paste),
                onClick = { focusedFrame().paste() },
                enabled = request.can(CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_PASTE),
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_select_all),
                onClick = { focusedFrame().selectAll() },
                enabled = request.can(CefContextMenuParams.EditStateFlags.CM_EDITFLAG_CAN_SELECT_ALL),
            )
        } else if (request.hasSelection) {
            DropdownMenuItem(label = stringResource(MR.strings.action_copy), onClick = { focusedFrame().copy() })
        }
        if (request.linkUrl == null && !request.editable && !request.hasSelection) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_webview_back),
                onClick = page::goBack,
                enabled = page.canGoBack,
            )
            DropdownMenuItem(
                label = stringResource(MR.strings.action_webview_forward),
                onClick = page::goForward,
                enabled = page.canGoForward,
            )
            DropdownMenuItem(label = stringResource(MR.strings.action_webview_refresh), onClick = page::reload)
        }
    }
}

/**
 * Chromium's request for a right-click menu at [x], [y] on the page, in CSS pixels, for what was clicked there. It waits
 * for one answer, which [dismiss] gives; the menu's own actions don't need Chromium's commands.
 */
internal class PageMenuRequest private constructor(
    val browser: CefBrowser,
    val x: Int,
    val y: Int,
    val linkUrl: String?,
    val hasSelection: Boolean,
    val editable: Boolean,
    private val editFlags: Int,
    private val callback: CefRunContextMenuCallback,
) {
    private var answered = false

    fun can(editFlag: Int) = editFlags and editFlag != 0

    fun dismiss() {
        if (answered) return
        answered = true
        callback.cancel()
    }

    companion object {
        fun of(browser: CefBrowser, params: CefContextMenuParams, callback: CefRunContextMenuCallback) =
            PageMenuRequest(
                browser = browser,
                x = params.xCoord,
                y = params.yCoord,
                linkUrl = params.linkUrl?.takeIf { it.isNotEmpty() },
                hasSelection = !params.selectionText.isNullOrEmpty(),
                editable = params.isEditable,
                editFlags = params.editStateFlags,
                callback = callback,
            )
    }
}
