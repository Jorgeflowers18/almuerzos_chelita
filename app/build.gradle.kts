plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // KSP genera automáticamente el código de la base de datos a partir de las anotaciones de Room.
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.chelita.almuerzos"
    compileSdk = 35

    defaultConfig {
        // Identificador único de la app. NO cambiarlo nunca: si cambia,
        // Android la trata como otra app y no se puede actualizar encima.
        applicationId = "com.chelita.almuerzos"
        minSdk = 26
        targetSdk = 35
        // Subir versionCode en 1 cada vez que se entregue una APK nueva.
        versionCode = 1
        versionName = "0.1.0"
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
    buildFeatures {
        compose = true
    }
    testOptions {
        // Necesario para que Robolectric simule Android en las pruebas de la base de datos.
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    // Room guarda aquí una "foto" del esquema de cada versión de la base de datos.
    // Se sube a git: sirve para escribir y probar migraciones cuando cambie el esquema.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
