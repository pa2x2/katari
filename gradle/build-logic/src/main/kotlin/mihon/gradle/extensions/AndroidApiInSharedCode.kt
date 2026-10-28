package mihon.gradle.extensions

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Lets the shared code of an extension-facing API name the Android classes extensions link against (`android.net.Uri`,
 * `SharedPreferences`, `androidx.preference.PreferenceScreen`, …), so its signatures keep the exact Android types on
 * every target. Compile-only: Android supplies the classes from the platform and the AndroidX dependency, desktop from
 * the extension host's Android stand-ins. Only extension-facing APIs and the extension host, which hands extensions
 * those types and reads them back, call this; everywhere else the compiler keeps rejecting Android APIs in shared code.
 */
fun Project.compileSharedCodeAgainstAndroidApi() {
    val androidComponents = extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>()
    val bootClasspath = androidComponents.sdkComponents.bootClasspath
    val preference = libs.androidx.preferenceBase.get()
    val preferenceAar = configurations.detachedConfiguration(
        dependencies.create("${preference.module}:${preference.versionConstraint.requiredVersion}@aar"),
    ).apply { isTransitive = false }
    val preferenceClasses = files(
        provider { zipTree(preferenceAar.singleFile).matching { include("classes.jar") }.files },
    )
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("commonMain") {
            dependencies {
                compileOnly(files(bootClasspath, preferenceClasses))
            }
        }
    }
}
