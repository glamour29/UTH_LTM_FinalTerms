plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // id("com.google.gms.google-services") version "4.4.0" apply false // Not using Firebase - using Socket.IO
}

android {
    namespace = "com.example.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.client"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       
        // Hỗ trợ 16 KB page size cho Android 15+
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // BẬT obfuscation
            isShrinkResources = true // (khuyến nghị)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
   
    // Cấu hình để hỗ trợ 16 KB page size (bắt buộc cho Android 15+)
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
   
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // ===== Main =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.socket.io.client)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.zxing.core)
   
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
   
    // 1. Để gọi API (Retrofit + Gson) - BẮT BUỘC, em giữ nguyên không xóa
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
   
    // 2. Để load ảnh Avatar (Glide) - Dùng cho Profile sau này, em giữ nguyên
    implementation("com.github.bumptech.glide:glide:4.16.0")
   
    // Socket.IO client (version cụ thể từ main branch, em giữ nguyên)
    implementation("io.socket:socket.io-client:2.1.0")

    // ===== Test =====
    testImplementation(libs.junit)
   
    // ===== Android Test =====
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
   
    // ===== Debug =====
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
