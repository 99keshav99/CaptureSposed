plugins {
    alias(libs.plugins.app)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "com.keshav.capturesposed"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.keshav.capturesposed"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
}