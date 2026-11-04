import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Build script for the single app module.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

/*
 * Release signing settings, read from the git-ignored local.properties so the
 * keystore path and passwords are never committed:
 *   release.storeFile=/home/<you>/keystores/plant-watch-release.jks
 *   release.storePassword=...
 *   release.keyAlias=plantwatch
 *   release.keyPassword=...
 * Without them (e.g. on another machine) the release APK is simply unsigned.
 *
 * Kotlin notes: `Properties().apply { ... }` creates the object and configures it
 * in one expression; `use { }` closes the file even if loading fails.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStoreFile: String? = localProperties.getProperty("release.storeFile")

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

    signingConfigs {
        // Only defined when this machine has the keystore settings.
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("release.storePassword")
                keyAlias = localProperties.getProperty("release.keyAlias")
                keyPassword = localProperties.getProperty("release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks and optimizes the code and removes unused resources:
            // a smaller, faster APK. Libraries ship their own keep rules; the app's
            // own extra rules (if ever needed) go in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // `findByName` returns null when no signing config was created above.
            signingConfig = signingConfigs.findByName("release")
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
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

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
