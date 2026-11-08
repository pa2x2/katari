// The sync server as the showcase runs it (see ShowcaseSyncServer.kt). tools/showcase/pipeline/sync starts the
// distribution installDist lays out.
plugins {
    alias(mihonx.plugins.kotlin.jvm)
    alias(mihonx.plugins.spotless)
    application
}

application {
    applicationName = "showcase-sync-server"
    mainClass = "app.katari.showcase.sync.ShowcaseSyncServerKt"
}

dependencies {
    implementation(projects.features.sync.server)
}
