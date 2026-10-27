plugins {
    alias(mihonx.plugins.desktop.application)
    alias(mihonx.plugins.spotless)
}

dependencies {
    implementation(libs.compose.multiplatform.ui)
}

compose.desktop.application {
    mainClass = "mihon.desktop.MainKt"
}
