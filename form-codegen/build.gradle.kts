plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

version = "1.0.6"

dependencies {
    implementation(libs.ksp.symbol.processing.api)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "form-codegen"
            from(components["java"])
        }
    }
}
