plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

group = "com.github.fede-debe"
version = "1.0.4"

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
