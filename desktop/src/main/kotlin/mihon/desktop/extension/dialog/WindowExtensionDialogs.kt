package mihon.desktop.extension.dialog

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mihon.core.common.web.ExtensionPage
import mihon.extension.compat.ExtensionDialogs
import mihon.extension.compat.ShownDialog

/**
 * The extensions' dialogs the app's window shows ([ExtensionDialogsHost]), in the order they were shown, the last on
 * top. Extensions show and change them on their main looper's thread; the window reads them on its own.
 */
@Inject
@SingleIn(AppScope::class)
class WindowExtensionDialogs : ExtensionDialogs {
    private val shown = MutableStateFlow<List<ExtensionDialog>>(emptyList())

    val dialogs: StateFlow<List<ExtensionDialog>> = shown.asStateFlow()

    override fun show(
        page: ExtensionPage,
        title: CharSequence?,
        cancelable: Boolean,
        canceledOnTouchOutside: Boolean,
        onCloseRequest: Runnable,
    ): ShownDialog = Shown(page, onCloseRequest).also { it.update(title, cancelable, canceledOnTouchOutside) }

    private inner class Shown(private val page: ExtensionPage, private val onCloseRequest: Runnable) : ShownDialog {
        @Volatile
        private var dismissed = false

        override fun update(title: CharSequence?, cancelable: Boolean, canceledOnTouchOutside: Boolean) {
            val dialog = ExtensionDialog(
                key = this,
                page = page,
                title = title?.toString(),
                closable = cancelable,
                closesOnClickOutside = canceledOnTouchOutside,
                requestClose = onCloseRequest::run,
            )
            shown.update { dialogs ->
                when {
                    dismissed -> dialogs
                    dialogs.any { it.key === this } -> dialogs.map { if (it.key === this) dialog else it }
                    else -> dialogs + dialog
                }
            }
        }

        override fun dismiss() {
            dismissed = true
            shown.update { dialogs -> dialogs.filterNot { it.key === this } }
        }
    }
}

/**
 * One extension's dialog showing [page]. The user can close it only where it is [closable], then by clicking outside
 * it too where it [closesOnClickOutside]; closing calls [requestClose], and the dialog stays until its extension
 * dismisses it.
 */
class ExtensionDialog(
    val key: Any,
    val page: ExtensionPage,
    val title: String?,
    val closable: Boolean,
    val closesOnClickOutside: Boolean,
    val requestClose: () -> Unit,
)
