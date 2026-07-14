package mihon.data.extension.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

class NetworkExtensionStoreTest {

    @Test
    fun `repository source metadata maps known entry types and ignores unknown values`() {
        val available = extensionList(
            extensionLib = "2.0.0",
            supportedEntryTypes = listOf("MANGA", "FUTURE_TYPE", "anime"),
        ).toAvailableExtensions(store).single()

        available.sources.single().supportedEntryTypes shouldBe setOf(EntryType.MANGA, EntryType.ANIME)
    }

    @Test
    fun `legacy extension families default to manga metadata`() {
        val available = extensionList(
            extensionLib = "1.6",
            supportedEntryTypes = emptyList(),
        ).toAvailableExtensions(store).single()

        available.sources.single().supportedEntryTypes shouldBe setOf(EntryType.MANGA)
    }

    @Test
    fun `entry extension without repository metadata remains unknown`() {
        val available = extensionList(
            extensionLib = "2.0.0",
            supportedEntryTypes = emptyList(),
        ).toAvailableExtensions(store).single()

        available.sources.single().supportedEntryTypes shouldBe null
    }

    private fun extensionList(
        extensionLib: String,
        supportedEntryTypes: List<String>,
    ) = NetworkExtensionStore.ExtensionList(
        extensions = listOf(
            NetworkExtensionStore.Extension(
                name = "Example",
                packageName = "example.extension",
                resources = NetworkExtensionStore.Resources(
                    apkUrl = "https://example.invalid/example.apk",
                    iconUrl = "https://example.invalid/example.png",
                ),
                extensionLib = extensionLib,
                versionCode = 1L,
                versionName = "$extensionLib.1",
                contentWarning = NetworkExtensionStore.ContentWarning.SAFE,
                sources = listOf(
                    NetworkExtensionStore.Source(
                        id = 1L,
                        name = "Example",
                        language = "en",
                        supportedEntryTypes = supportedEntryTypes,
                    ),
                ),
            ),
        ),
    )

    private companion object {
        val store = ExtensionStore(
            indexUrl = "https://example.invalid/index.json",
            name = "Example",
            badgeLabel = "Example",
            signingKey = "",
            contact = ExtensionStore.Contact(
                website = "https://example.invalid",
                discord = null,
            ),
            isLegacy = false,
        )
    }
}
