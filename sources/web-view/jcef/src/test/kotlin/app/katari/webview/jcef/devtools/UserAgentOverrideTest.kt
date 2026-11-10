package app.katari.webview.jcef.devtools

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class UserAgentOverrideTest {

    // The app's own user agent is a phone's Chrome, on a desktop's Chromium of another version
    @Test
    fun `a phone's Chrome is a phone's Chrome in its platform and hints too`() {
        val override = userAgentOverride(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36",
        )

        override["platform"]!!.jsonPrimitive.content shouldBe "Linux armv81"
        val hints = override["userAgentMetadata"]!!.jsonObject
        hints["platform"]!!.jsonPrimitive.content shouldBe "Android"
        hints["platformVersion"]!!.jsonPrimitive.content shouldBe "10.0.0"
        hints["mobile"]!!.jsonPrimitive.boolean shouldBe true
        hints["brands"]!!.jsonArray.map { it.jsonObject["version"]!!.jsonPrimitive.content } shouldBe
            listOf("149", "149", "24")
    }

    @Test
    fun `a user agent that isn't Chrome's gets no Chrome hints`() {
        val override = userAgentOverride("Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0")

        override["platform"]!!.jsonPrimitive.content shouldBe "Linux x86_64"
        override.containsKey("userAgentMetadata") shouldBe false
    }
}
