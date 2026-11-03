package eu.kanade.tachiyomi.ui.webview.browser

/**
 * A page's `alert`, `confirm` or `prompt`, which the page waits on until it is answered. [url] is the page asking, null
 * when the browser didn't say; [cancel] answers as closing the dialog would.
 */
sealed class BrowserJsDialog(val url: String?, val message: String, private val onCancel: () -> Unit) {
    fun cancel() = onCancel()

    class Alert(url: String?, message: String, private val onConfirm: () -> Unit, onCancel: () -> Unit) :
        BrowserJsDialog(url, message, onCancel) {
        fun confirm() = onConfirm()
    }

    class Confirm(url: String?, message: String, private val onConfirm: () -> Unit, onCancel: () -> Unit) :
        BrowserJsDialog(url, message, onCancel) {
        fun confirm() = onConfirm()
    }

    /** Asks for text, offering [defaultValue]. */
    class Prompt(
        url: String?,
        message: String,
        val defaultValue: String,
        private val onConfirm: (String) -> Unit,
        onCancel: () -> Unit,
    ) : BrowserJsDialog(url, message, onCancel) {
        fun confirm(value: String) = onConfirm(value)
    }
}
