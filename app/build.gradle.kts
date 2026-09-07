plugins {
    id("com.android.application")
}

android {
    namespace = "com.masaryamamah.otpbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.masaryamamah.otpbridge"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = false
        }
    }
}