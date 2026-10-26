plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.codominator.angusmasail"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codominator.angusmasail"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PROD_URL", "\"https://angus.iamshahid.pro/\"")
        buildConfigField("String", "DEV_URL", "\"http://127.0.0.1:8080/\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8080/\"")
            buildConfigField("Boolean", "IS_PROD", "false")
            buildConfigField("String", "ENVIRONMENT", "\"DEV\"")
            versionNameSuffix = "-dev"
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://angus.iamshahid.pro/\"")
            buildConfigField("Boolean", "IS_PROD", "true")
            buildConfigField("String", "ENVIRONMENT", "\"PROD\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Location & Play Services
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
