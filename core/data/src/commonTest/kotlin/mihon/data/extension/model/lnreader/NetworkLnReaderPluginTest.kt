package mihon.data.extension.model.lnreader

import eu.kanade.tachiyomi.extension.model.ListedDownload
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.identity.lnReaderExternalId

class NetworkLnReaderPluginTest {

    private val json = Json { ignoreUnknownKeys = true }

    // Arabic plugins listed under "other" behind their left-to-right mark, a plugin installed without its style sheet,
    // and Nekori's or a self-hosted index's plugins offered for install though Katari can't run them
    @Test
    fun `a listing takes its language, sdk version and style sheet from the index`() {
        val arabic = listing(ARNOVEL)
        arabic.lang shouldBe "ar"
        arabic.pkgName shouldBe "lnreader:arnovel"
        arabic.libVersionName shouldBe "3.0.0"
        arabic.sources.single().id shouldBe lnReaderExternalId("arnovel")

        (listing(NOVELUPDATES).download as ListedDownload.File).companions shouldBe mapOf(
            "custom.css" to "$PLUGINS/public/static/src/en/novelupdates/customCSS.css",
        )

        listing(NOVELUPDATES.replace("/plugins/v3.0.0/.js/", "/master/.js/")).libVersionName shouldBe ""
        listing(NOVELUPDATES.replace("\"id\"", "\"isNekoriPlugin\":true,\"id\"")).libVersionName shouldBe ""
    }

    private fun listing(entry: String) = json.decodeFromString<NetworkLnReaderPlugin>(entry).toAvailableExtension(STORE)

    private companion object {
        const val LEFT_TO_RIGHT_MARK = "\u200E"
        const val PLUGINS = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0"

        val STORE = ExtensionStore(
            indexUrl = "$PLUGINS/.dist/plugins.min.json",
            name = "LNReader",
            badgeLabel = "LNR",
            signingKey = null,
            contact = ExtensionStore.Contact(website = "https://github.com/LNReader/lnreader-plugins", discord = null),
            format = ExtensionStoreFormat.LNREADER,
        )

        // From the official plugins/v3.0.0 index
        val ARNOVEL = """
            {"id":"arnovel","name":"ArNovel","site":"https://ar-no.com/","lang":"${LEFT_TO_RIGHT_MARK}العربية",
            "version":"2.3.0",
            "url":"$PLUGINS/.js/src/plugins/arabic/ArNovel[madara].js",
            "iconUrl":"$PLUGINS/public/static/multisrc/madara/arnovel/icon.png"}
        """
        val NOVELUPDATES = """
            {"id":"novelupdates","name":"Novel Updates","site":"https://www.novelupdates.com/","lang":"English",
            "version":"0.9.9","url":"$PLUGINS/.js/src/plugins/english/novelupdates.js",
            "iconUrl":"$PLUGINS/public/static/src/en/novelupdates/icon.png",
            "customCSS":"$PLUGINS/public/static/src/en/novelupdates/customCSS.css"}
        """
    }
}
