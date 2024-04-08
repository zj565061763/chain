plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sd.demo.chain"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        targetSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = 21
        applicationId = "com.sd.demo.chain"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    implementation(project(":lib"))
}