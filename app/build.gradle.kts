import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Build script for the single app module.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.alberto.plantwatch" // package for the generated R and BuildConfig classes

    compileSdk {
        // APIs we compile against. Current AndroidX libraries require 37.
        // This does NOT change runtime behavior (that's targetSdk) nor which
        // devices can install the app (that's minSdk).
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.alberto.plantwatch" // unique ID of the installed app
        minSdk = 34
        targetSdk = 35                  // matches the phone (Android 15)
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // Java bytecode level 17 (we compile with JDK 21, but target 17).
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

room {
    // Room writes a JSON description of each database version here. It's committed
    // to git: it documents the schema and is the reference for writing migrations.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // platform(...) imports the BOM so the Compose libraries below need no versions.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Room: `implementation` = library code shipped in the app;
    // `ksp` = code generator that runs only at build time (not shipped).
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Only in debug builds: renders @Preview and the layout inspector.
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Unit tests (app/src/test): run on the computer's JVM, no phone needed.
    testImplementation(libs.junit)
}
