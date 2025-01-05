
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    viewBinding {
        enable = true // Исправьте строку 10 таким образом, чтобы использовать assignment, а не comparison
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }




    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation ("com.yandex.android:mobileads:7.8.1")


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
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

