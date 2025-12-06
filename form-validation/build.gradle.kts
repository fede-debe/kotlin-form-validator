plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

group = "com.github.fede-debe"
version = "1.0.0"

android {
    namespace = "com.example.form.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26 // Required for java.time (LocalDate) support

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Coroutines (for StateFlow)
    implementation(libs.kotlinx.coroutines.android)

    // Compose Runtime (for @Stable annotation)
    implementation(libs.androidx.compose.runtime.annotation)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.fede-debe"
            artifactId = "form-validation"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Form Validation")
                description.set("Declarative, type-safe form validation library for Android with Jetpack Compose")
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