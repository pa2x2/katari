package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.collections.shouldContainExactly
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

class ExtensionManagerTest {

    @Test
    fun `available extensions are filtered by supported extension lib versions`() {
        val extensions = listOf(
            availableExtension(name = "Legacy 1.4", libVersionName = "1.4"),
            availableExtension(name = "Legacy 1.6", libVersionName = "1.6"),
            availableExtension(name = "Old fork 1.10", libVersionName = "1.10.0"),
            availableExtension(name = "Entry 2.0", libVersionName = "2.0.0"),
            availableExtension(name = "Entry 2.0 patch", libVersionName = "2.0.1"),
            availableExtension(name = "Entry 2.1", libVersionName = "2.1.0"),
        )

        extensions
            .filterCompatibleWithApp()
            .map { it.name } shouldContainExactly listOf(
            "Legacy 1.4",
            "Legacy 1.6",
            "Entry 2.0",
            "Entry 2.0 patch",
        )
    }

    private fun availableExtension(
        name: String,
        libVersionName: String,
    ) = Extension.Available(
        name = name,
        pkgName = "pkg.${name.lowercase().replace(' ', '.')}",
        versionName = "$libVersionName.0",
        versionCode = 1L,
        libVersion = libVersionName.toLibVersionDouble(),
        lang = "en",
        isNsfw = false,
        sources = emptyList(),
        apkUrl = "",
        iconUrl = "",
        store = extensionStore,
        libVersionName = libVersionName,
    )

    private fun String.toLibVersionDouble(): Double {
        val parts = split('.')
        return if (parts.size >= 2) {
            "${parts[0]}.${parts[1]}".toDouble()
        } else {
            toDouble()
        }
    }

    private companion object {
        val extensionStore = ExtensionStore(
            indexUrl = "https://example.org/repo.json",
            name = "Test store",
            badgeLabel = "test",
            signingKey = "",
            contact = ExtensionStore.Contact(
                website = "https://example.org",
                discord = null,
            ),
            isLegacy = false,
        )
    }
}
