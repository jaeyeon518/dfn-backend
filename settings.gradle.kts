plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "backend-test"

include(
    // App
    "app:data-ingestion",
    "app:data-producer",

    // Model
    "model",

    // Library
    "library:generator"
)

project(":app:data-ingestion").projectDir = file("app/data-ingestion")
project(":app:data-producer").projectDir = file("app/data-producer")
project(":library:generator").projectDir = file("library/generator")
