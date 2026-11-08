package app.katari.sync.ui.page

import java.io.Serializable

/** The dialog or sheet the Sync page shows, kept across a fold or a recreated activity. */
internal sealed interface SyncPageDialog : Serializable {
    data object LinkDevice : SyncPageDialog

    data object RenameDevice : SyncPageDialog

    data class RemoveDevice(val id: Long, val name: String) : SyncPageDialog

    data object Disconnect : SyncPageDialog

    data object ServerAddress : SyncPageDialog

    data object MissingExtensions : SyncPageDialog
}
