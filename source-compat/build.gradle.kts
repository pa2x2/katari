import mihon.gradle.tasks.PrepareLegacyFixtureTask
import org.gradle.api.tasks.testing.Test

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

val legacy14FixtureJar = layout.buildDirectory.file("legacy-fixtures/legacy14-fixture.jar")
val decodeLegacy14Fixture = tasks.register<PrepareLegacyFixtureTask>("decodeLegacy14Fixture") {
    encodedFixture.set(layout.projectDirectory.file("fixtures/upstream14/legacy14-fixture.jar.b64"))
    outputFile.set(legacy14FixtureJar)
}

android {
    namespace = "eu.kanade.tachiyomi.source.compat"
}

dependencies {
    api(projects.entrySourceApi)
    api(projects.sourceApi)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(files(legacy14FixtureJar))
}

tasks.withType<Test>().configureEach {
    dependsOn(decodeLegacy14Fixture)
}
