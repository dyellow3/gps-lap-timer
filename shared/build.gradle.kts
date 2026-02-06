plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.gpslaptimer.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api("com.google.android.gms:play-services-maps:18.2.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    testImplementation(libs.junit)
}
