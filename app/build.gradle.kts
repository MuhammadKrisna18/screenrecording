plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.screenrecorder.app"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.screenrecorder.app"
    minSdk = 29
    targetSdk = 37
    versionCode = 1
    versionName = "1.0.0"
  }

  signingConfigs {
    create("release") {
      storeFile = file("keystore/release.jks")
      storePassword = "screenrecorder"
      keyAlias = "screenrecorder"
      keyPassword = "screenrecorder"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)

  debugImplementation(libs.androidx.compose.ui.tooling)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
