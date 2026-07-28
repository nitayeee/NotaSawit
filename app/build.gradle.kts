plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.notasawit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.notasawit"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures{
        viewBinding = true
        compose = false // <-- Wajib dipastikan true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 1. Supabase BOM (Untuk menyamakan versi otomatis)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))

    // 2. Modul Supabase Auth (Untuk fitur Login/OTP)
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // 3. Ktor Client (Mesin penggerak jaringan yang diwajibkan oleh Supabase Kotlin)
    implementation("io.ktor:ktor-client-android:3.0.0")

    // 4. Ini untuk sidik jari
    implementation("androidx.biometric:biometric:1.1.0")

    // 5. Untuk dot indicator
    implementation("com.tbuonomo:dotsindicator:5.1.0")

//    6. Tambahkan Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

//    7. Untuk carousel
//    implementation("com.google.android.material:material:1.9.0")

//    8. Untuk Server PCR
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

//    9. Ini untuk ambil data array yang desa
    implementation("com.google.code.gson:gson:2.10.1")

//    10. Untuk sign in
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")

    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.android.material:material:1.12.0")

//    11. Untuk quote
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

//    12. Ambil gambar
    implementation("com.github.bumptech.glide:glide:4.16.0")

//    13. Untuk Room database local
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp ("androidx.room:room-compiler:$room_version")

    //Untuk View model
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")

//    Untuk Work Manager
    implementation("androidx.work:work-runtime-ktx:2.10.2")

    // untuk audit lahan
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Chart Library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}