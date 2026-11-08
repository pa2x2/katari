// The sync server as the showcase runs it (see ShowcaseSyncServer.kt). tools/showcase/pipeline/sync starts the
// distribution installDist lays out.
plugins {
    alias(katari.plugins.kotlin.jvm)
    alias(katari.plugins.spotless)
    application
}

application {
    applicationName = "showcase-sync-server"
    mainClass = "app.katari.showcase.sync.ShowcaseSyncServerKt"
}

dependencies {
    implementation(projects.features.sync.server)
}
