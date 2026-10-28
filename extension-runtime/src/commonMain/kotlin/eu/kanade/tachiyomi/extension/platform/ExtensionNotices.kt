package eu.kanade.tachiyomi.extension.platform

/** What the extension manager tells the user outside its screens. */
interface ExtensionNotices {

    /** The stores couldn't be reached when the user asked for their listings. Called on the main thread. */
    fun storesUnreachable()

    /** [names] were updated without asking; none withdraws the notice. */
    fun autoUpdated(names: List<String>)

    /** [names] have updates to install; none withdraws the notice. */
    fun promptUpdates(names: List<String>)

    /** Withdraws both update notices, once nothing has an update. */
    fun dismissUpdates()

    /** A restored backup's file extensions: [installed] again and [missed], which couldn't be. Not both empty. */
    fun backupExtensionsInstalled(installed: List<String>, missed: List<String>)
}
