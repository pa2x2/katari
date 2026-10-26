package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
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

    private fun untrustedExtension() = Extension.NotLoaded(
        name = "Untrusted",
        pkgName = "untrusted.extension",
        versionName = "1.0",
        versionCode = 1L,
        form = InstalledForm.Package(isShared = true, signatures = listOf("signature")),
        contentWarning = ContentWarning.SAFE,
        libVersion = 2.0,
        reason = Extension.NotLoaded.Reason.Untrusted("signature"),
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
        format = ExtensionStoreFormat.V2,
    )
}
