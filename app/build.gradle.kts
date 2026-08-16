import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.regibazarapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.regibazarapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google Maps API Key handling
        val localProperties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        val mapsApiKey = (project.findProperty("MAPS_API_KEY") as? String) 
            ?: localProperties.getProperty("MAPS_API_KEY") 
            ?: ""
            
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore (secure token storage)
    implementation(libs.androidx.datastore.preferences)

    // Networking: Retrofit2 + OkHttp
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Google Maps for Compose
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    // Location (GPS)
    implementation(libs.play.services.location)

    // SignalR Real-Time
    implementation(libs.signalr)

    // Image Loading
    implementation(libs.coil.compose)

    // Runtime Permissions
    implementation(libs.accompanist.permissions)

    // Security & Biometrics
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Room — Modo Búnker (caché offline SQLite)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager — sync silencioso en background
    implementation(libs.work.runtime.ktx)

    // Glance — Widget de pantalla de inicio (Compose para AppWidgets)
    implementation(libs.glance.appwidget)

    // Firebase — Push Notifications (FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // ML Kit — Escaneo QR / Código de barras
    implementation(libs.mlkit.barcode.scanning)

    // CameraX — Preview para el escáner QR
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Lottie — Animaciones premium
    implementation(libs.lottie.compose)

    // Haze — Blur real (glassmorphism)
    implementation(libs.haze)
    implementation(libs.haze.materials)

    // Google Fonts en Compose (Poppins)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Generación local de QR y CODE-128 para las etiquetas térmicas publicadas.
    implementation(libs.zxing.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
