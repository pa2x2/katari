package mihon.gradle.extensions

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Lets the shared code of a module that extensions link against, or that hands extensions Android's types and reads
 * them back, name the Android classes involved (`android.net.Uri`, `SharedPreferences`,
 * `androidx.preference.PreferenceScreen`, …), so its signatures keep the exact Android types on every target. Called
 * by the extension-facing APIs, the extension host, the source adapters (`source-compat`) and the test doubles of
 * those types; everywhere else the compiler keeps rejecting Android APIs in shared code.
 *
 * Compile-only: Android supplies the classes from the platform and the AndroidX dependency, desktop from the extension
 * host's Android stand-ins.
 */
fun Project.compileSharedCodeAgainstAndroidApi() {
    val androidApi = androidApiClasses(libs.androidx.preferenceBase)
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("commonMain") {
            dependencies {
                compileOnly(androidApi)
            }
        }
    }
}

/**
 * Lets the desktop code of an extension-facing API declare the members extensions link against with the signatures
 * they have on Android, which name Android classes and the classes of [androidLibraries] (such as UniFile). Compile-only:
 * on desktop the extension host's Android stand-ins supply Android's classes, and a library's classes are absent, so a
 * member taking one of them can't be reached there.
 */
fun Project.compileDesktopCodeAgainstAndroidApi(vararg androidLibraries: Provider<MinimalExternalModuleDependency>) {
    val androidApi = androidApiClasses(*androidLibraries)
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("desktopMain") {
            dependencies {
                compileOnly(androidApi)
            }
        }
    }
}

/** The platform's `android.jar` and the classes of the Android libraries [libraries], taken out of their AARs. */
private fun Project.androidApiClasses(vararg libraries: Provider<MinimalExternalModuleDependency>): FileCollection {
    val androidComponents = extensions.getByType<KotlinMultiplatformAndroidComponentsExtension>()
    val bootClasspath = androidComponents.sdkComponents.bootClasspath
    val aars = configurations.detachedConfiguration(
        *libraries.map { library ->
            val module = library.get()
            dependencies.create("${module.module}:${module.versionConstraint.requiredVersion}@aar")
        }.toTypedArray(),
    ).apply { isTransitive = false }
    val libraryClasses = files(
        provider {
            aars.files.flatMap { aar -> zipTree(aar).matching { include("classes.jar") }.files }
        },
    )
    return files(bootClasspath, libraryClasses)
}
