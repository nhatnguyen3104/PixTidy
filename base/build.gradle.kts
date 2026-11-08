
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    FileInputStream(secretsFile).use { secrets.load(it) }
}

android {
    namespace = "com.androidtech.base"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "admob_app_id", "\"${secrets.getProperty("ID_ADMOB_Prod") ?: "ca-app-pub-3940256099942544~3347511713"}\"")
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
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // OkHttp
    implementation(libs.com.squareup.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    // Retrofit
    implementation (libs.com.squareup.retrofit)
    implementation (libs.com.squareup.retrofit.converter.gson)
    // Timber
    implementation(libs.com.jakewharton.timber)
    // Hilt
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    // Moshi
    implementation(libs.com.squareup.moshi.kotlin)
    implementation(libs.com.squareup.retrofit2.converter.moshi)
    // Analytic
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.analytics)
    // Lottie
    implementation(libs.lottie.compose)
    // Serialization
    implementation(libs.org.jetbrains.kotlinx.serialization.json)
    // App Update
    implementation(libs.com.google.android.play.app.update)
    implementation(libs.com.google.android.play.app.update.ktx)
}