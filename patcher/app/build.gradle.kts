plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.dazaike.ciderpatcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dazaike.ciderpatcher"
        minSdk = 33
        targetSdk = 34          // unchanged on purpose: raising it changes platform behaviour
        versionCode = 4
        versionName = "1.0.3"
    }

    buildTypes { release { isMinifyEnabled = false } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            "META-INF/DEPENDENCIES",
            "/META-INF/{AL2.0,LGPL2.1}",
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.compose.runtime:runtime:1.12.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.1")
    implementation("org.jetbrains.compose.animation:animation:1.12.1")
    implementation("org.jetbrains.compose.ui:ui:1.12.1")
    implementation("org.jetbrains.compose.ui:ui-graphics:1.12.1")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
