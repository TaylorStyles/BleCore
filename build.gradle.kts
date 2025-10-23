plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

group = "com.stylesdevelopments.blecore"
version = "1.0.0"

android {
    namespace = "com.stylesdevelopments.blecore"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // Kotlin + Coroutines
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Nordic BLE
    implementation(libs.nordic.ble.core)
    implementation(libs.nordic.ble.client)
    implementation(libs.nordic.ble.scanner)
}