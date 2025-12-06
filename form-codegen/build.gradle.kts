import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

group = "com.github.fede-debe"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
    withJavadocJar()
}

// IMPORTANT: Ensure Kotlin compiles to the same JVM target as Java
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.fede-debe"
            artifactId = "form-codegen"
            version = "1.0.0"

            from(components["java"])

            pom {
                name.set("Form Code Generator")
                description.set("KSP processor for generating type-safe form mappers")
                url.set("https://github.com/fede-debe/kotlin-form-validator")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("fede-debe")
                        name.set("Federico")
                        email.set("federico.debenedictis33@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/fede-debe/kotlin-form-validator.git")
                    developerConnection.set("scm:git:ssh://github.com/fede-debe/kotlin-form-validator.git")
                    url.set("https://github.com/fede-debe/kotlin-form-validator")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/fede-debe/kotlin-form-validator")
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = findProperty("gpr.key") as String? ?: System.getenv("GPR_KEY")
            }
        }
    }
}