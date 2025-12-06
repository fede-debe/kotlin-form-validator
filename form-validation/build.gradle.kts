plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

version = "1.0.6"

android {
    namespace = "com.github.form.validation"
    compileSdk = 34

    defaultConfig {
        minSdk = 26 // Required for java.time (LocalDate) support
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 1. Coroutines (for StateFlow)
    implementation(libs.kotlinx.coroutines.android)

    // 2. Compose Runtime (for @Stable annotation)
    implementation(libs.androidx.compose.runtime.annotation)

    // 3. Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = "form-validation"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
