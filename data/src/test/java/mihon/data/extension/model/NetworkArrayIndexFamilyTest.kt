package mihon.data.extension.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Test

class NetworkArrayIndexFamilyTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Each family has its own trust rule, so reading a store as the wrong one, or accepting an index that mixes them,
    // would trust extensions the user never agreed to
    @Test
    fun `an array index is a family's only when every entry is one of that family's and none another's`() {
        json.arrayIndexFamily(read("[$LNREADER_ENTRY]")) shouldBe ArrayIndexFamily.LNREADER
        json.arrayIndexFamily(read("[$KEIYOUSHI_ENTRY]")) shouldBe ArrayIndexFamily.LEGACY

        val mixed = read("[$LNREADER_ENTRY,$LNREADER_ENTRY,$KEIYOUSHI_ENTRY]")
        shouldThrow<IllegalArgumentException> { json.arrayIndexFamily(mixed) }
            .message shouldStartWith "Entry 3 isn't an LNReader plugin"
        shouldThrow<IllegalArgumentException> { json.arrayIndexFamily(read("[]")) }
            .message shouldBe "This store lists no extensions"
    }

    private fun read(index: String) = json.decodeFromString<JsonArray>(index)

    private companion object {
        const val LEFT_TO_RIGHT_MARK = "\u200E"
        const val PLUGINS = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0"

        // From the official plugins/v3.0.0 index, with the left-to-right mark its Arabic names start with
        val LNREADER_ENTRY = """
            {"id":"arnovel","name":"ArNovel","site":"https://ar-no.com/","lang":"${LEFT_TO_RIGHT_MARK}العربية",
            "version":"2.3.0",
            "url":"$PLUGINS/.js/src/plugins/arabic/ArNovel[madara].js",
            "iconUrl":"$PLUGINS/public/static/multisrc/madara/arnovel/icon.png"}
        """

        // From Keiyoushi's index.min.json
        val KEIYOUSHI_ENTRY = """
            {"name":"Outdated App","pkg":"eu.kanade.tachiyomi.extension.all.keiyoushi",
            "apk":"tachiyomi-all.keiyoushi-v1.4.1.apk","lang":"all","code":1,"version":"1.4.1","nsfw":0,
            "sources":[{"name":"Outdated App","lang":"all","id":"1","baseUrl":"https://keiyoushi.github.io"}]}
        """
    }
}
