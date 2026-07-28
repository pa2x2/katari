package mihon.translation.provider.libretranslate.offline

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OfflineTranslatorConfigurationTest {

    @Test
    fun `ports are accepted only in the TCP port range`() {
        OfflineTranslatorConfiguration.parsePort("1") shouldBe 1
        OfflineTranslatorConfiguration.parsePort(" 5000 ") shouldBe 5000
        OfflineTranslatorConfiguration.parsePort("65535") shouldBe 65535
        OfflineTranslatorConfiguration.parsePort("0") shouldBe null
        OfflineTranslatorConfiguration.parsePort("65536") shouldBe null
        OfflineTranslatorConfiguration.parsePort("not-a-port") shouldBe null
    }
}
