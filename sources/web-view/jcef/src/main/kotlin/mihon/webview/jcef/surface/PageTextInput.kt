package mihon.webview.jcef.surface

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Opens the window's input method to [page] while the page has focus, as to a text field of the app's: the input
 * method then composes and commits text, dead keys and the compose key make accented letters, and its windows sit at
 * the page's caret. What it composes goes to Chromium through [ChromiumPage.composition]; the keys it leaves reach the
 * page as key events.
 */
internal fun Modifier.pageTextInput(page: ChromiumPage): Modifier = this then PageTextInputElement(page)

private data class PageTextInputElement(val page: ChromiumPage) : ModifierNodeElement<PageTextInputNode>() {
    override fun create() = PageTextInputNode(page)

    override fun update(node: PageTextInputNode) {
        node.page = page
    }
}

private class PageTextInputNode(var page: ChromiumPage) :
    Modifier.Node(),
    PlatformTextInputModifierNode,
    FocusEventModifierNode,
    GlobalPositionAwareModifierNode {
    private var session: Job? = null
    private var positionInRoot = Offset.Zero

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused) {
            if (session?.isActive != true) {
                session = coroutineScope.launch {
                    establishTextInputSession { startInputMethod(PageInputMethodRequest(page, ::caretInRoot)) }
                }
            }
        } else {
            page.composition.finish()
            session?.cancel()
            session = null
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        positionInRoot = coordinates.positionInRoot()
    }

    private fun caretInRoot(): Rect? {
        val caret = page.caret.bounds ?: return null
        val scale = page.frame.size.scale.toFloat()
        return Rect(caret.left * scale, caret.top * scale, caret.right * scale, caret.bottom * scale)
            .translate(positionInRoot)
    }
}

/** The page as the input method's client: the text it composes, and the caret its windows sit at. */
@OptIn(ExperimentalComposeUiApi::class)
private class PageInputMethodRequest(page: ChromiumPage, caretInRoot: () -> Rect?) : PlatformTextInputMethodRequest {
    private val composition = page.composition

    override val state: TextEditorState
        get() = composition.state

    override val editText: (block: TextEditingScope.() -> Unit) -> Unit = composition::edit

    override val focusedRectInRoot: () -> Rect? = caretInRoot

    // The rest serves the platforms' other input paths and text fields of Compose's own, which the page isn't
    override val value: () -> TextFieldValue = { TextFieldValue(composition.state.toString()) }
    override val imeOptions: ImeOptions = ImeOptions.Default
    override val onEditCommand: (List<EditCommand>) -> Unit = {}
    override val onImeAction: ((ImeAction) -> Unit)? = null
    override val textLayoutResult: () -> TextLayoutResult? = { null }
    override val textFieldRectInRoot: () -> Rect? = caretInRoot
    override val textClippingRectInRoot: () -> Rect? = { null }
    override val unclippedTextOffsetInRoot: () -> Offset? = { null }
}
