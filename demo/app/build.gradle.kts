plugins {
  id("com.android.application")
  id("kotlin-android")
  id("androidx.navigation.safeargs.kotlin")
}

android {
  namespace = "com.google.fhir.examples.demo"
  compileSdk = 33
  defaultConfig {
    applicationId = "com.google.fhir.examples.demo"
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["appAuthRedirectScheme"] = applicationId!!
    buildFeatures.buildConfig = true
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  buildFeatures { viewBinding = true }
  compileOptions {
    // Flag to enable support for the new language APIs
    // See https://developer.android.com/studio/write/java8-support
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlin { jvmToolchain(11) }
  packaging { resources.excludes.addAll(listOf("META-INF/ASL-2.0.txt", "META-INF/LGPL-3.0.txt")) }
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
  implementation("androidx.activity:activity-ktx:1.7.2")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation("androidx.fragment:fragment-ktx:1.6.0")
  implementation("androidx.recyclerview:recyclerview:1.3.0")
  implementation("androidx.work:work-runtime-ktx:2.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
  implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
  implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation("com.google.android.material:material:1.9.0")
  implementation("com.jakewharton.timber:timber:5.0.1")
  implementation("net.openid:appauth:0.11.1")
  implementation("com.auth0.android:jwtdecode:2.0.1")
  implementation("com.google.android.fhir:engine:0.1.0-beta02")
  implementation("com.google.android.fhir:data-capture:1.0.0")
}
