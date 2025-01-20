
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
    packaging {
        resources {
            pickFirst("META-INF/NOTICE.md")
            pickFirst("META-INF/LICENSE.md")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
        }
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
    implementation (libs.material.v150)
    implementation (libs.android.mail)
    implementation (libs.android.activation)
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
    implementation ("org.postgresql:postgresql:42.2.27")
    implementation ("io.github.jan-tennert.supabase:postgrest-kt:1.4.7")
    implementation ("io.github.jan-tennert.supabase:gotrue-kt:1.4.7")
    implementation ("io.ktor:ktor-client-android:2.3.7")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

