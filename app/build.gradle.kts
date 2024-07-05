import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}



android {
    namespace = "com.example.gpslaptimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gpslaptimer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API key from secrets.properties
        val secretsPropertiesFile = rootProject.file("secrets.properties")
        val secretsProperties = Properties()
        if (secretsPropertiesFile.exists()) {
            secretsProperties.load(FileInputStream(secretsPropertiesFile))
            manifestPlaceholders["apiKey"] = secretsProperties["API_KEY"] as Any
        }
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

    buildFeatures {

        viewBinding = true
    }
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel.android)
    implementation(libs.legacy.support.v4)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
}