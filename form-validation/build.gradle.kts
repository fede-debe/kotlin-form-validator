plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

group = "com.github.fede-debe"
version = "1.0.1"

android {
    namespace = "com.example.form.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.compose.runtime.annotation)
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
                description.set("Declarative form validation library")
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
                    url.set("https://github.com/fede-debe/kotlin-form-validator")
                }
            }
        }
    }
}