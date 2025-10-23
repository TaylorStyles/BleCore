plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
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

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.stylesdevelopments.blecore"
            artifactId = "blecore"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("BleCore")
                description.set("A lightweight BLE abstraction layer for Android.")
                url.set("https://github.com/taylorstyles/BleCore")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("taystyles")
                        name.set("Taylor Styles")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/taylorstyles/BleCore.git")
                    developerConnection.set("scm:git:ssh://github.com/taylorstyles/BleCore.git")
                    url.set("https://github.com/taylorstyles/BleCore")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/taylorstyles/BleCore")

            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
