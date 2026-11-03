package mihon.desktop.portal

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RequestHandlePathTest {

    // A path that differs from the portal's still works once the call returns its own, so only a Response arriving
    // before the call returns, which the match added beforehand exists for, would be lost
    @Test
    fun `the request path is the one the portal makes from the connection's unique name`() {
        requestHandlePath(":1.4207", "katari3") shouldBe "/org/freedesktop/portal/desktop/request/1_4207/katari3"
    }
}
