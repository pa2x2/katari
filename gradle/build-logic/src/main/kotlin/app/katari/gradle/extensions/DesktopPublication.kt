package app.katari.gradle.extensions

import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.withType

/**
 * Publishes a multiplatform SDK module without its desktop variant. Extensions are built for Android and converted on
 * desktop, so nothing consumes a desktop variant, and its dependencies (the Android stand-ins) stay out of the SDK.
 * The root module metadata still lists the variant; only an unpublished desktop consumer would follow it.
 */
fun Project.skipDesktopPublication() {
    // By name: the configuration cache does not keep a publish task's publication for an execution-time check
    tasks.withType<AbstractPublishToMaven>()
        .named { it.startsWith("publishDesktopPublicationTo") }
        .configureEach { enabled = false }
}
