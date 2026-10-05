import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Chave de assinatura do APK (fica fora do git: keystore.properties + subaru-player.jks)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.subaruplayer.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.subaruplayer.app"
        // Android 10+: o MediaStore informa a pasta de cada vídeo (RELATIVE_PATH)
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "LIBVLC_VERSION", "\"${libs.versions.libvlc.get()}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Celulares (arm64) e o emulador (x86_64): o resto só engordaria o APK
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // O libVLC chama o código Java a partir do C (JNI): o R8 renomearia e quebraria o player
            isMinifyEnabled = false
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        disable += "IconLauncherShape"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.libvlc)

    // Testes instrumentados dos gestos do player (rodam no emulador/celular: ./gradlew connectedDebugAndroidTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    // O Espresso que vem junto é velho demais para o Android 17 (usa InputManager.getInstance, que não existe mais)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
