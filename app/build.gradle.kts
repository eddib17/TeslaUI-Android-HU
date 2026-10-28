plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.eddib17.actyhu"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.eddib17.actyhu"
        minSdk = 29 // Android 10; the Tab S10+ ships with Android 14+
        targetSdk = 35
        // CI sets GITHUB_RUN_NUMBER, so every CI build has a higher versionCode and installs as an update.
        val ciBuild = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = ciBuild ?: 1
        versionName = if (ciBuild != null) "0.1.0-ci.$ciBuild" else "0.1.0-local"
    }

    signingConfigs {
        // A fixed debug key checked into the repo. Without it, each CI machine signs with its own random key
        // and Android refuses to install a new APK over the old one. NOT secret and never for a store release.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    lint {
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
