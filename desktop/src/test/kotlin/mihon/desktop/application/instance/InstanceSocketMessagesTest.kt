package mihon.desktop.application.instance

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class InstanceSocketMessagesTest {

    // An app updated while it runs answers launches of the new version, and a launch of the old one can still reach a
    // newer instance: a request either can't read loses the launch's links and leaves the window behind
    @Test
    fun `requests of older and newer launches are read`() {
        InstanceSocketMessages.decode("""{"activationToken":"older"}""".encodeToByteArray()) shouldBe
            InstanceRequest(activationToken = "older")
        val newer = """{"activationToken":"newer","links":["katari://extension-store?url=x"],"later":{"field":1}}"""
        InstanceSocketMessages.decode(newer.encodeToByteArray()) shouldBe
            InstanceRequest(activationToken = "newer", links = listOf("katari://extension-store?url=x"))
    }
}
