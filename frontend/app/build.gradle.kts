plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "fr.uge.android.forkeat"
  compileSdk = 36

  defaultConfig {
    applicationId = "fr.uge.android.forkeat"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      storeFile = file(System.getProperty("user.home") + "/forkeat-release.keystore")
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
      keyAlias = "forkeat"
      keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  buildFeatures {
    compose = true
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  coreLibraryDesugaring(libs.desugarJdkLibs)
  implementation(project(":core:designsystem"))
  implementation(project(":core:network"))
  implementation(project(":data:promotions"))
  implementation(project(":feature:home"))
  implementation(project(":feature:profile"))
  implementation(project(":data:recipes"))
  implementation(project(":feature:recipes"))
  implementation(project(":feature:wallet"))
  implementation(project(":feature:account"))
  implementation(project(":feature:admin"))
  implementation(project(":feature:moderator"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.navigation.compose)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}