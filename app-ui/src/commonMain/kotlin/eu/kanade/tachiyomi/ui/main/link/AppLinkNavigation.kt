package eu.kanade.tachiyomi.ui.main.link

import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen

/**
 * Opens [link]'s screen from the home's root. Nothing is restored or added from here: the restore screen and the
 * extension store's confirmation ask first.
 */
fun Navigator.openAppLink(link: AppLink) {
    popUntilRoot()
    push(
        when (link) {
            is AppLink.RestoreBackup -> RestoreBackupScreen(link.backup)
            is AppLink.Entry -> EntryScreen(link.entryId, fromSource = true)
            is AppLink.AddExtensionStore -> ExtensionStoresScreen(link.indexUrl)
        },
    )
}
