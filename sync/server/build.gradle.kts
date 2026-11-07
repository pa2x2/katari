// The self-hosted synchronization server: an HTTP API over one SQLite database and a folder of files in a data
// directory, serving the protocol of :sync:protocol to every device of every account it holds. One artifact both serves
// and carries the recovery command (see app.katari.sync.server.Main).
plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.sqldelight)
    application
}

// The server's own version, shown on its web page and released apart from the apps.
version = "1.0.0-beta.1"

application {
    applicationName = "katari-sync-server"
    mainClass = "app.katari.sync.server.MainKt"
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

sqldelight {
    databases {
        create("SyncServerDatabase") {
            packageName.set("app.katari.sync.server.storage.sqlite")
            dialect(libs.sqldelight.sqliteDialect338)
        }
    }
}

dependencies {
    implementation(projects.sync.protocol)

    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sqldelight.sqliteDriver)
    implementation(libs.bouncycastle.prov)
    runtimeOnly(libs.slf4j.simple)
    constraints {
        // Ktor brings kotlin-reflect at the Kotlin it was built with; reflection must match the stdlib it runs on.
        implementation(libs.kotlin.reflect)
    }

    testImplementation(libs.bundles.test)
    testImplementation(libs.ktor.server.testHost)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}
