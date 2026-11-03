package mihon.feature.appupdate.check

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReleaseVersionTest {

    @Test
    fun `a release candidate is older than its release and a lower hotfix is older than a newer minor`() {
        fun newer(candidate: String, installed: String) =
            ReleaseVersion.parse(candidate)!! > ReleaseVersion.parse(installed)!!

        // Stripping non-digits once turned 1.13.0-rc1 into 1.13.01, so rc users were never offered 1.13.0.
        newer("v1.13.0", "1.13.0-rc1") shouldBe true
        newer("v1.13.0-rc2", "1.13.0-rc1") shouldBe true
        newer("v1.13.0-rc.10", "1.13.0-rc.9") shouldBe true
        // Comparing part by part and stopping at the first larger one offered 1.11.5 over 1.12.0.
        newer("v1.11.5", "1.12.0") shouldBe false
        newer("v1.12.0", "1.12.0") shouldBe false
    }
}
