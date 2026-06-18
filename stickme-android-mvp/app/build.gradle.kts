import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties()
val hasReleaseKeystore = releaseKeystorePropertiesFile.exists()

if (hasReleaseKeystore) {
    releaseKeystoreProperties.load(FileInputStream(releaseKeystorePropertiesFile))
}

fun releaseProperty(name: String): String? =
    releaseKeystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

fun projectVersionCode(): Int =
    (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1

fun projectVersionName(): String =
    (findProperty("VERSION_NAME") as String?)?.takeIf { it.isNotBlank() } ?: "0.1.0"

android {
    namespace = "com.stickme.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stickme.app"
        minSdk = 26
        targetSdk = 35
        versionCode = projectVersionCode()
        versionName = projectVersionName()
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(releaseProperty("storeFile") ?: "secrets/stickme-upload-key.jks")
                storePassword = releaseProperty("storePassword")
                keyAlias = releaseProperty("keyAlias") ?: "upload"
                keyPassword = releaseProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.core:core:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
