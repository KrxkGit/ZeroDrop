plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.zerodrop.app"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.zerodrop.app"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    lint {
        disable.add("NullSafeMutableLiveData")
        abortOnError = true
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
            isDebuggable = true
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

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Compose for Wear OS
    val wearComposeVersion = "1.6.2"
    implementation("androidx.wear.compose:compose-material3:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")

    // Wear OS
    implementation("androidx.wear:wear:1.4.0")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.wear:wear-ongoing:1.1.0")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.core:core-ktx:1.15.0")

    // DataStore for persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.3")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
