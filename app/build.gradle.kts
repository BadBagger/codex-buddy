plugins {
    id("com.android.application")
}

android {
    namespace = "com.softsmith.codexbuddy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.softsmith.codexbuddy"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.1-panel-history"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
