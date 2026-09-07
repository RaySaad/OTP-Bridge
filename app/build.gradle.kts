plugins {
    id("com.android.application")
}

android {
    namespace = "com.masaryamamah.otpbridge"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.masaryamamah.otpbridge"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
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
