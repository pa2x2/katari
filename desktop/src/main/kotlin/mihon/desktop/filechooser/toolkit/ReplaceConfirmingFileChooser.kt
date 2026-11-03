package mihon.desktop.filechooser.toolkit

import tachiyomi.i18n.MR
import tachiyomi.i18n.action_cancel
import tachiyomi.i18n.file_chooser_replace
import tachiyomi.i18n.file_chooser_replace_message
import tachiyomi.i18n.file_chooser_replace_title
import tachiyomi.i18n.localized.stringResource
import javax.swing.JFileChooser
import javax.swing.JOptionPane

/**
 * Swing's chooser, which on its own saves over a file that exists without a word; this one asks first, as the
 * desktop's own choosers do.
 */
internal class ReplaceConfirmingFileChooser : JFileChooser() {
    override fun approveSelection() {
        val file = selectedFile
        if (dialogType == SAVE_DIALOG && file != null && file.exists() && !confirmReplacing(file.name, file.parent)) {
            return
        }
        super.approveSelection()
    }

    private fun confirmReplacing(name: String, folder: String?): Boolean {
        val options = arrayOf(stringResource(MR.strings.file_chooser_replace), stringResource(MR.strings.action_cancel))
        // The index of the option chosen, or CLOSED_OPTION; Cancel is the default, as replacing loses the file
        val answer = JOptionPane.showOptionDialog(
            this,
            stringResource(MR.strings.file_chooser_replace_message, folder.orEmpty()),
            stringResource(MR.strings.file_chooser_replace_title, name),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[CANCEL],
        )
        return answer == REPLACE
    }

    private companion object {
        const val REPLACE = 0
        const val CANCEL = 1
    }
}
