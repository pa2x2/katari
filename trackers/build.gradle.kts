import mihon.gradle.extensions.compileSharedCodeAgainstAndroidApi
import mihon.gradle.extensions.desktop
import mihon.gradle.getAppVersionName

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apollo)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "mihon.trackers"
        // The trackers' logos are Compose resources, which Android packages as the module's assets.
        androidResources {
            enable = true
        }

        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(projects.domain)
                api(projects.entrySourceApi)
                api(projects.i18n)
                api(libs.compose.multiplatform.resources)

                implementation(projects.core.platform)

                // The tracking host backs up and restores tracks through the database directly.
                implementation(projects.data)
                implementation(projects.entryInteractions.api)

                implementation(libs.androidx.annotation)
                implementation(libs.apollo)
                implementation(libs.apollo.adapters)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.serialization)
                implementation(libs.kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        androidMain {
            dependencies {
                implementation(projects.core.metro)
                implementation(libs.androidx.work)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.jsoup)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "mihon.trackers.resources"
    // The tracking screens' previews show a tracker's logo without a tracker.
    publicResClass = true
}

apollo {
    val schemaBasePath = "src/commonMain/graphql/mihon/graphql"
    service("anilist") {
        packageName.set("mihon.graphql.anilist")
        val srcDir = "$schemaBasePath/anilist"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("$srcDir/anilist.graphqls"))
        }

        // ISO 3166-1 alpha-2 country code
        mapScalarToKotlinString("CountryCode")
    }

    service("kitsu") {
        packageName.set("mihon.graphql.kitsu")
        val srcDir = "$schemaBasePath/kitsu"
        srcDir(file(srcDir))

        introspection {
            // Kitsu doesn't like requests without a UA
            headers.put("User-Agent", getAppVersionName().map { "Katari v$it (app.katari)" })
            endpointUrl.set("https://kitsu.app/api/graphql")
            schemaFile.set(file("$srcDir/kitsu.graphqls"))
        }

        // A date, expressed as an ISO8601 string
        mapScalarToKotlinString("Date")
        // An ISO 8601-encoded datetime
        mapScalar("ISO8601DateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
        // A loose key-value map in GraphQL
        mapScalar(
            "Map",
            "kotlin.collections.Map<String, Any?>",
            "mihon.graphql.kitsu.adapter.KitsuMapAdapter",
        )
    }

    service("shikimori") {
        packageName.set("mihon.graphql.shikimori")
        val srcDir = "$schemaBasePath/shikimori"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("https://shikimori.io/api/graphql")
            schemaFile.set(file("$srcDir/shikimori.graphqls"))
        }

        // An ISO 8601-encoded date
        mapScalarToKotlinString("ISO8601Date")
    }

    service("suwayomi") {
        packageName.set("mihon.graphql.suwayomi")
        val srcDir = "$schemaBasePath/suwayomi"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("http://localhost:4567/api/graphql")
            schemaFile.set(file("$srcDir/suwayomi.graphqls"))
        }
    }
}

// The self-hosted trackers read their extension source's SharedPreferences
compileSharedCodeAgainstAndroidApi()
