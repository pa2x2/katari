package tachiyomi.data.release

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReleaseApkSelectionTest {

    @Test
    fun `FOSS builds update to the FOSS APK for the device ABI`() {
        abis.forEach { abi ->
            selectReleaseApk(release, isFoss = true, primaryAbi = abi)?.name shouldBe "katari_foss_$abi-v2.0.0.apk"
        }
    }

    @Test
    fun `FOSS builds fall back to the universal FOSS APK when the release has none for the device ABI`() {
        val withoutFossAbiApks = release.filterNot { it.name.startsWith("katari_foss_") }

        selectReleaseApk(withoutFossAbiApks, isFoss = true, primaryAbi = "arm64-v8a")?.name shouldBe
            "katari-v2.0.0-foss.apk"
    }

    @Test
    fun `standard builds update to the standard APK for the device ABI`() {
        abis.forEach { abi ->
            selectReleaseApk(release, isFoss = false, primaryAbi = abi)?.name shouldBe "katari-$abi-v2.0.0.apk"
        }
    }

    @Test
    fun `standard builds fall back to the universal standard APK when the release has none for the device ABI`() {
        val withoutArm64Apk = release.filterNot { it.name == "katari-arm64-v8a-v2.0.0.apk" }

        selectReleaseApk(withoutArm64Apk, isFoss = false, primaryAbi = "arm64-v8a")?.name shouldBe "katari-v2.0.0.apk"
    }

    @Test
    fun `devices whose ABI is not built are offered no APK`() {
        listOf("x86", "x86_64").forEach { abi ->
            selectReleaseApk(release, isFoss = false, primaryAbi = abi).shouldBeNull()
            selectReleaseApk(release, isFoss = true, primaryAbi = abi).shouldBeNull()
        }
    }

    private val abis = listOf("arm64-v8a", "armeabi-v7a")

    private val release = listOf(
        "katari-v2.0.0.apk",
        "katari-arm64-v8a-v2.0.0.apk",
        "katari-armeabi-v7a-v2.0.0.apk",
        "katari-v2.0.0-foss.apk",
        "katari_foss_arm64-v8a-v2.0.0.apk",
        "katari_foss_armeabi-v7a-v2.0.0.apk",
    ).map { GitHubAsset(name = it, downloadLink = "https://example.org/$it") }
}
