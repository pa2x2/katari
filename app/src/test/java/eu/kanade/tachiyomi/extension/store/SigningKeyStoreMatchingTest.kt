package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

class SigningKeyStoreMatchingTest {

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
        isShared = true,
        signatures = signatures,
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
        apkUrl = "https://${store.name}.invalid/example.apk",
        iconUrl = "",
        store = store,
        libVersionName = libVersionName,
        sourceIdScheme = SourceIdScheme.TACHIYOMI,
        isTorrent = false,
    )

    private fun store(name: String, signingKey: String) = ExtensionStore(
        indexUrl = "https://$name.invalid/index.json",
        name = name,
        badgeLabel = name,
        signingKey = signingKey,
        contact = ExtensionStore.Contact(website = "https://$name.invalid", discord = null),
        format = ExtensionStoreFormat.V2,
    )

    private companion object {
        const val PKG_NAME = "example.extension"
    }
}
