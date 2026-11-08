plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.com.google.devtools.ksp)
}

android {
    namespace = "com.ads.monetization"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(project(path = ":data"))
    implementation(project(path = ":domain"))
    implementation(project(path = ":base"))

    // Coroutine
    implementation(libs.org.jetbrains.kotlinx.coroutines)
    // Inject
    implementation(libs.javax.inject)
    // Hilt
    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    // Admob
    implementation(libs.ads.mobile.sdk)
    // Moshi
    implementation(libs.com.squareup.moshi.kotlin)
    implementation(libs.com.squareup.retrofit2.converter.moshi)

}