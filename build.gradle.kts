/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.application") version "8.1.2"
    id("org.jetbrains.kotlin.android") version "1.7.10"
}

android {
    compileSdk = 34
    namespace = "org.lineageos.eleven"

    defaultConfig {
        applicationId = "org.lineageos.eleven"
        minSdk = 28
        targetSdk = 34
        versionCode = 420
        versionName = "4.2.0"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
        }
        getByName("release") {
            // Enables code shrinking, obfuscation, and optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            // Includes the default ProGuard rules files.
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard.cfg"
                )
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")

            aidl.srcDirs("src")
            assets.srcDirs("assets")
            java.srcDirs("src")
            res.srcDirs("res")
            resources.srcDirs("res")
        }
    }

    buildFeatures {
        aidl = true
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
        checkAllWarnings = true
        showAll = true
        warningsAsErrors = true
        xmlReport = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
}
