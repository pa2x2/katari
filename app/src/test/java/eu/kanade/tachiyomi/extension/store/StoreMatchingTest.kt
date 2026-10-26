package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import eu.kanade.tachiyomi.extension.model.ListedDownload
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

class StoreMatchingTest {

    @Test
    fun `updates come only from stores whose signing key the installed extension is signed with`() {
        val installed = installed(versionCode = 2, signatures = listOf("key-a"))
        val olderMatching = available(store("a", signingKey = "key-a"), versionCode = 3)
        val newerMatching = available(store("a-mirror", signingKey = "key-a"), versionCode = 4)
        val newestForeign = available(store("b", signingKey = "key-b"), versionCode = 9)

        installed.findUpdate(listOf(olderMatching, newestForeign, newerMatching)) shouldBe newerMatching
        installed.findUpdate(listOf(newestForeign)) shouldBe null
    }

    @Test
    fun `a newer listing built with a library this app can't run isn't offered as an update`() {
        val installed = installed(versionCode = 2, signatures = listOf("key-a"))
        val runnable = available(store("a", signingKey = "key-a"), versionCode = 3)
        val newerLib = available(store("a-mirror", signingKey = "key-a"), versionCode = 4, libVersionName = "1.7")

        installed.findUpdate(listOf(runnable, newerLib)) shouldBe runnable
        installed.findUpdate(listOf(newerLib)) shouldBe null
    }

    // Nothing signs a plugin, so another store listing the same id could otherwise push its code as an update; and
    // compared as text or as LNReader does, 1.10 would never be newer than 1.9
    @Test
    fun `a plugin updates only from the store it was installed from, by the numbers of its dotted version`() {
        val installed = installedPlugin(versionName = "1.9.0", storeIndexUrl = store("a", signingKey = null).indexUrl)
        val newer = availablePlugin(store("a", signingKey = null), versionName = "1.10.0")
        val newestElsewhere = availablePlugin(store("b", signingKey = null), versionName = "2.0.0")

        installed.findUpdate(listOf(newestElsewhere, newer)) shouldBe newer
        installed.findUpdate(listOf(availablePlugin(store("a", signingKey = null), versionName = "1.9"))) shouldBe null
    }

    private fun installedPlugin(versionName: String, storeIndexUrl: String) = Extension.NotLoaded(
        name = "Novel Updates",
        pkgName = PLUGIN_PKG_NAME,
        versionName = versionName,
        versionCode = 0,
        form = InstalledForm.File(sha256 = "0".repeat(64), storeIndexUrl = storeIndexUrl),
        contentWarning = ContentWarning.NOT_RATED,
        reason = Extension.NotLoaded.Reason.Filtered,
    )

    private fun availablePlugin(store: ExtensionStore, versionName: String) = Extension.Available(
        name = "Novel Updates",
        pkgName = PLUGIN_PKG_NAME,
        versionName = versionName,
        versionCode = 0,
        libVersion = 3.0,
        lang = "en",
        contentWarning = ContentWarning.NOT_RATED,
        sources = emptyList(),
        download = ListedDownload.File("https://${store.name}.invalid/novelupdates.js"),
        iconUrl = "",
        store = store,
        libVersionName = "3.0.0",
        sourceIdScheme = SourceIdScheme.LNREADER,
        isTorrent = false,
    )

    private fun installed(versionCode: Long, signatures: List<String>) = Extension.Loaded(
        name = "Example",
        pkgName = PKG_NAME,
        versionName = "1.6.$versionCode",
        versionCode = versionCode,
        libVersion = 1.6,
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        pkgFactory = null,
        sources = emptyList(),
        sourceIdScheme = SourceIdScheme.TACHIYOMI,
        providerKind = SourceProviderKind.TACHIYOMI_EXTENSION,
        form = InstalledForm.Package(isShared = true, signatures = signatures),
    )

    private fun available(
        store: ExtensionStore,
        versionCode: Long,
        libVersionName: String = "1.6",
    ) = Extension.Available(
        name = "Example",
        pkgName = PKG_NAME,
        versionName = "$libVersionName.$versionCode",
        versionCode = versionCode,
        libVersion = libVersionName.toDouble(),
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        sources = emptyList(),
        download = ListedDownload.Apk("https://${store.name}.invalid/example.apk"),
        iconUrl = "",
        store = store,
        libVersionName = libVersionName,
        sourceIdScheme = SourceIdScheme.TACHIYOMI,
        isTorrent = false,
    )

    private fun store(name: String, signingKey: String?) = ExtensionStore(
        indexUrl = "https://$name.invalid/index.json",
        name = name,
        badgeLabel = name,
        signingKey = signingKey,
        contact = ExtensionStore.Contact(website = "https://$name.invalid", discord = null),
        format = ExtensionStoreFormat.V2,
    )

    private companion object {
        const val PKG_NAME = "example.extension"
        const val PLUGIN_PKG_NAME = "lnreader:novelupdates"
    }
}
