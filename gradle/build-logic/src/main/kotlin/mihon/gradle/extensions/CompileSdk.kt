package mihon.gradle.extensions

import com.android.build.api.dsl.CompileSdkSpec
import org.gradle.api.Project

/**
 * Resolves the compile SDK from the mihonx version catalog into the [CompileSdkSpec] DSL shared by
 * Android extensions and Kotlin Multiplatform Android targets.
 *
 * The catalog value is the full SDK version, e.g. "37.1" for API 37 with minor 1, or "38" for API
 * 38 without a minor version.
 */
internal fun Project.compileSdkFromMihonx(): CompileSdkSpec.() -> Unit {
    val version = mihonx.versions.android.sdk.compile.get()
    val parts = version.split('.')
    val apiLevel = parts[0].toInt()
    val minorApiLevel = parts.getOrNull(1)?.toInt()
    return {
        this.version = if (minorApiLevel == null) {
            release(apiLevel)
        } else {
            release(apiLevel) { this.minorApiLevel = minorApiLevel }
        }
    }
}
