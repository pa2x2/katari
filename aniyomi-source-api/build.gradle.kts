plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.aniyomi.source"

    defaultConfig {
        consumerProguardFiles("consumer-proguard.pro")
    }
}

dependencies {
    api(projects.core.common)
    api(libs.injekt)
    api(libs.jsoup)
    api(libs.nanohttpd)
}
