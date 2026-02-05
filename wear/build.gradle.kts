plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.gpslaptimer.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gpslaptimer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.appcompat)
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
