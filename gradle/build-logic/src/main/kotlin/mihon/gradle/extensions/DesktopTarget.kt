package mihon.gradle.extensions

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds the desktop JVM target, whose sources live in `src/desktopMain`. Multiplatform modules call it once they compile
 * for desktop; when every shared module does, `PluginKotlinMultiplatform` adds the target itself and the calls go.
 */
fun KotlinMultiplatformExtension.desktop() {
    jvm("desktop")
}
