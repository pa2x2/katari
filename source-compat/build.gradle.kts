import org.gradle.api.tasks.testing.Test
import java.util.Base64

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

val legacy14FixtureJar = layout.buildDirectory.file("legacy-fixtures/legacy14-fixture.jar")
val decodeLegacy14Fixture = tasks.register("decodeLegacy14Fixture") {
    val encodedFixture = layout.projectDirectory.file("fixtures/upstream14/legacy14-fixture.jar.b64")
    inputs.file(encodedFixture)
    outputs.file(legacy14FixtureJar)

    doLast {
        val output = legacy14FixtureJar.get().asFile
        output.parentFile.mkdirs()
        output.writeBytes(Base64.getMimeDecoder().decode(encodedFixture.asFile.readBytes()))
    }
}

android {
    namespace = "eu.kanade.tachiyomi.source.compat"
}

dependencies {
    api(projects.entrySourceApi)
    api(projects.sourceApi)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(files(legacy14FixtureJar))
}

tasks.withType<Test>().configureEach {
    dependsOn(decodeLegacy14Fixture)
}
