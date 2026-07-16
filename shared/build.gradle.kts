plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // iOS targets (iosArm64, iosSimulatorArm64) are added when the iOS port begins;
    // they require a macOS host to compile. commonMain stays pure Kotlin so the
    // existing code compiles for iOS unchanged. See docs/IOS_PORT.md.
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // api: AppSettings takes a Settings in its public constructor.
            api(libs.multiplatform.settings)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.core.ktx)
        }
    }
}

android {
    namespace = "com.zakir.vestra.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
