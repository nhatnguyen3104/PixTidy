plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
}

android {
    namespace = "com.androidtech.generalapps"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.androidtech.generalapps"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.create("release") {
                keyAlias = "generalapps"
                keyPassword = "generalapps@123"
                storeFile = file("key/release.jks")
                storePassword = "generalapps@123"
            }
        }

    }

    flavorDimensions += "default"
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
        }

        create("prod") {
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(project(path = ":domain"))
    implementation(project(path = ":data"))
    implementation(project(path = ":base"))
    implementation(project(path = ":feature"))
    implementation(project(path = ":monetization"))

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)
    // Timber
    implementation(libs.com.jakewharton.timber)
    // Coil
    implementation(libs.io.coil.kt.compose)
    // Work manager
//    implementation(libs.androidx.runtime.worker)
    // Exo player
//    implementation(libs.androidx.media3.exoplayer)
//    implementation(libs.androidx.media3.ui)
    // Moshi
    implementation(libs.com.squareup.moshi.kotlin)
    implementation(libs.com.squareup.retrofit2.converter.moshi)
    // Gson
    implementation(libs.com.google.code.gson)
    // Coroutine
    implementation(libs.org.jetbrains.kotlinx.coroutines)
    // OkHttp
    implementation(libs.com.squareup.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    // Room database
    implementation(libs.androidx.runtime.room)
    ksp(libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Live data
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    // Multi dex
    implementation(libs.androidx.multidex)
    // Retrofit
    implementation (libs.com.squareup.retrofit)
    implementation (libs.com.squareup.retrofit.converter.gson)
    // Skydoves
    implementation (libs.com.github.skydoves.sandwich)
    //
    implementation(libs.com.google.accompanist.systemuicontroller)
    // Hilt
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    // Permissions
    implementation(libs.android.permissions)
    // Serializable
    implementation(libs.org.jetbrains.kotlinx.serialization.json)
    // pager
    implementation(libs.accompanist.pager)
    // camera
//    implementation(libs.androidx.camera)
//    implementation(libs.camera.lifecycle)
//    implementation(libs.camera.view)
//    implementation(libs.camera.camera2)
    // Admob
    implementation(libs.play.services.ads)
    // Splash
    implementation(libs.androidx.core.splashscreen)
}