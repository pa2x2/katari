package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
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

    private fun available(store: ExtensionStore, versionCode: Long) = Extension.Available(
        name = "Example",
        pkgName = PKG_NAME,
        versionName = "1.6.$versionCode",
        versionCode = versionCode,
        libVersion = 1.6,
        lang = "en",
        contentWarning = ContentWarning.SAFE,
        sources = emptyList(),
        apkUrl = "https://${store.name}.invalid/example.apk",
        iconUrl = "",
        store = store,
        libVersionName = "1.6",
        sourceIdScheme = SourceIdScheme.TACHIYOMI,
        isTorrent = false,
    )

    private fun store(name: String, signingKey: String) = ExtensionStore(
        indexUrl = "https://$name.invalid/index.json",
        name = name,
        badgeLabel = name,
        signingKey = signingKey,
        contact = ExtensionStore.Contact(website = "https://$name.invalid", discord = null),
        isLegacy = false,
    )

    private companion object {
        const val PKG_NAME = "example.extension"
    }
}
