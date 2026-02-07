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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation("androidx.wear:wear:1.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
