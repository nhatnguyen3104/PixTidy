
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.com.google.gms.google.services)
    alias(libs.plugins.kotlin.serialization)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    FileInputStream(secretsFile).use { secrets.load(it) }
}

android {
    namespace = "com.androidtech.generalapps"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("tier")

    androidComponents {
        beforeVariants { variantBuilder ->
            val buildType = variantBuilder.buildType

            val allowBuildTypes = setOf(
                "devDebug",
                "devRelease",
                "prodDebug",
                "prodRelease"
            )

            if (buildType !in allowBuildTypes) {
                variantBuilder.enable = false
            }
        }
    }

    buildTypes {
        create("devDebug") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            manifestPlaceholders["admob_app_id"] = secrets.getProperty("ID_ADMOB_Dev") ?: "ca-app-pub-3940256099942544~3347511713"
        }

        create("devRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += "release"
            manifestPlaceholders["admob_app_id"] = secrets.getProperty("ID_ADMOB_Dev") ?: "ca-app-pub-3940256099942544~3347511713"
        }

        create("prodDebug") {
            initWith(getByName("debug"))
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "debug"
            manifestPlaceholders["admob_app_id"] = secrets.getProperty("ID_ADMOB_Prod") ?: "ca-app-pub-3940256099942544~3347511713"
        }

        create("prodRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += "release"
            manifestPlaceholders["admob_app_id"] = secrets.getProperty("ID_ADMOB_Prod") ?: "ca-app-pub-3940256099942544~3347511713"
        }
    }

    productFlavors {
        /*create("general") {
            dimension = "tier"
            applicationId = "com.androidtech.generalapps"
            versionCode = 1
            versionName = "1.0.0"
            resValue("string", "app_name", "General")

            buildConfigField("String", "DOMAIN_URL", "\"https://mobiledev-nct.tech/\"")
        }*/

        create("PixTidy") {
            dimension = "tier"
            applicationId = "com.androidtech.pixtidy"
            versionCode = 3
            versionName = "0.0.3"
            resValue("string", "app_name", "PixTidy")

            buildConfigField("String", "DOMAIN_URL", "\"https://mobiledev-nate.tech/\"")
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
    implementation(libs.androidx.appcompat)
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
    implementation(libs.io.coil.kt.video)
    // Work manager
//    implementation(libs.androidx.runtime.worker)
    // Exo player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
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
    // DataStore
    implementation(libs.androidx.datastore.preferences)
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
    implementation(libs.ads.mobile.sdk)
    // Firebase
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.analytics)
    implementation(libs.com.google.firebase.remote.configs)
    // App Update
    implementation(libs.com.google.android.play.app.update)
    implementation(libs.com.google.android.play.app.update.ktx)
}