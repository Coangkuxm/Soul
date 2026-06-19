plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.soul"
    compileSdk = 36
    
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.soul"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "spotify_client_id", "82355370414e43139adb470311e58d73")
        resValue("string", "spotify_redirect_uri", "soul://spotify-callback")
        manifestPlaceholders["redirectSchemeName"] = "soul"
        manifestPlaceholders["redirectHostName"] = "spotify-callback"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Retrofit để gọi API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp để log dữ liệu API (rất quan trọng khi debug)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // ViewModel & LiveData (MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Activity KTX for viewModels() delegate
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(files("libs/spotify-auth-release-2.1.0.aar"))
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // CircleImageView for avatar
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Media3 ExoPlayer for audio preview playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")

    // Socket.IO realtime messaging
    implementation("io.socket:socket.io-client:2.1.2") {
        exclude(group = "org.json", module = "json")
    }

    // Emoji picker giống Messenger (bảng emoji đầy đủ, có phân loại)
    implementation("androidx.emoji2:emoji2-emojipicker:1.4.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
