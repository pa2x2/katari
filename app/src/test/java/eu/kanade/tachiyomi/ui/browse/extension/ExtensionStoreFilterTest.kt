package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

class ExtensionStoreFilterTest {

    @Test
    fun `disabled stores that are no longer configured do not activate filter`() {
        val configuredStore = extensionStore("configured")
        val filter = ExtensionStoreFilter(
            stores = listOf(configuredStore),
            disabledStoreUrls = setOf("https://removed.invalid/index.json"),
        )

        filter.isActive shouldBe false
        filter.matches(untrustedExtension()) shouldBe true
    }

    private fun untrustedExtension() = Extension.Untrusted(
        name = "Untrusted",
        pkgName = "untrusted.extension",
        versionName = "1.0",
        versionCode = 1L,
        libVersion = 2.0,
        signatureHash = "signature",
    )

    private fun extensionStore(name: String) = ExtensionStore(
        indexUrl = "https://$name.invalid/index.json",
        name = name,
        badgeLabel = name,
        signingKey = "",
        contact = ExtensionStore.Contact(
            website = "https://$name.invalid",
            discord = null,
        ),
        isLegacy = false,
    )
}
