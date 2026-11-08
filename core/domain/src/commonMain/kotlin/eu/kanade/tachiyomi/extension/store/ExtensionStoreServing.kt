package eu.kanade.tachiyomi.extension.store

import app.katari.domain.extension.model.ExtensionStore
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm

/**
 * Whether [store]'s listings are of this very extension, as opposed to another build under the same name: an apk is
 * only replaced by one signed with the same key, and a file extension is updated only from the store it came from.
 */
fun Extension.Installed.isServedBy(store: ExtensionStore): Boolean {
    return when (val form = form) {
        is InstalledForm.Package -> store.signingKey in form.signatures
        is InstalledForm.File -> store.indexUrl == form.storeIndexUrl
    }
}
