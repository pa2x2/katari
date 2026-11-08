package app.katari.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.util.Properties

/**
 * The SDK's `android.jar` for the catalog's compile SDK, the platform Android and multiplatform modules compile
 * against. AGP gives those modules its boot class path, but a JVM module has no AGP to ask, so this looks for the SDK
 * where AGP does: `sdk.dir` in the build's `local.properties`, then `ANDROID_HOME`, then `ANDROID_SDK_ROOT`.
 */
fun Project.androidPlatformJar(): Provider<RegularFile> {
    val settingsDirectory = layout.settingsDirectory
    val localSdk = providers.fileContents(settingsDirectory.file("local.properties")).asText
        .map { text -> Properties().apply { load(text.reader()) }.getProperty("sdk.dir") }
    val sdk = localSdk
        .orElse(providers.environmentVariable("ANDROID_HOME"))
        .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
    val platform = "android-${katari.versions.android.sdk.compile.get()}"
    return settingsDirectory.file(sdk.map { "$it/platforms/$platform/android.jar" })
}
