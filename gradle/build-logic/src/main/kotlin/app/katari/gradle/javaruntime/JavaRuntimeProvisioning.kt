package app.katari.gradle.javaruntime

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Lets one task at a time in a build provision a given runtime. It holds no state: with a single usage allowed at once,
 * Gradle runs the tasks that use it one after another.
 */
abstract class JavaRuntimeProvisioning : BuildService<BuildServiceParameters.None>

/** The build's [JavaRuntimeProvisioning] for the runtime unpacked to [runtime]'s home, registered by the first caller. */
internal fun Project.javaRuntimeProvisioning(runtime: PinnedJavaRuntime): Provider<JavaRuntimeProvisioning> =
    gradle.sharedServices.registerIfAbsent(
        "provision-${runtime.home.name}",
        JavaRuntimeProvisioning::class.java,
    ) { maxParallelUsages.set(1) }
