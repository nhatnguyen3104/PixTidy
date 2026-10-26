plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
}

android {
    namespace = "com.androidtech.data"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.vision.common)

    implementation(project(path = ":domain"))

    // moshi
    implementation(libs.com.squareup.moshi.kotlin)
    ksp(libs.com.squareup.moshi.kotlin.codegen)
    implementation(libs.com.squareup.retrofit2.converter.moshi)
    // Retrofit
    implementation (libs.com.squareup.retrofit)
    implementation (libs.com.squareup.retrofit.converter.gson)
    // Coroutine
    implementation(libs.org.jetbrains.kotlinx.coroutines)
    // Inject
    implementation(libs.javax.inject)
    // Room database
    implementation(libs.androidx.runtime.room)
    ksp(libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // remote config
    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.com.google.firebase.remote.configs)
    // Hilt
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.android.compiler)
    // Timber
    implementation(libs.com.jakewharton.timber)
}