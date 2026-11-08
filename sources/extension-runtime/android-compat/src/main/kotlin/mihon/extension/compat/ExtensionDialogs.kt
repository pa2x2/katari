package mihon.extension.compat

import mihon.core.common.web.ExtensionPage

/**
 * The app's window as an extension's `android.app.Dialog` over the foreground activity reaches it: the dialog opens as
 * one of the app's own, with the page of the web view that is the extension's content in it. How it looks is the app's;
 * what an extension sets on the dialog's window never reaches it. Called on the dialog's thread, the main looper's.
 */
interface ExtensionDialogs {
    /**
     * Shows [page] until [ShownDialog.dismiss]. While [cancelable], the user can close the dialog, and with
     * [canceledOnTouchOutside] also by clicking outside it, which only calls [onCloseRequest], on a thread of the
     * window's: the dialog stays until its extension dismisses it.
     */
    fun show(
        page: ExtensionPage,
        title: CharSequence?,
        cancelable: Boolean,
        canceledOnTouchOutside: Boolean,
        onCloseRequest: Runnable,
    ): ShownDialog
}

interface ShownDialog {
    /** Takes on what the extension changed of the dialog it shows. */
    fun update(title: CharSequence?, cancelable: Boolean, canceledOnTouchOutside: Boolean)

    fun dismiss()
}
