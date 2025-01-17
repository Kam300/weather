
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    viewBinding {
        enable = true // Исправьте строку 10 таким образом, чтобы использовать assignment, а не comparison
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    namespace = "com.example.weathertyre"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.weathertyre"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }




    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation (libs.mobileads)
    implementation ("com.google.android.material:material:1.5.0")

    implementation(libs.androidx.core.ktx) // Up-to-date version
    implementation(libs.okhttp) // Ensure this points to the correct version
    implementation(libs.gson) // Ensure this points to the correct version
    implementation(libs.androidx.drawerlayout)
    implementation(libs.glide)
    implementation(libs.gms.play.services.location) // Если не требуется, также можно удалить
    implementation(libs.cardview)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation (libs.okhttp.v4120)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

