plugins {
  id(Plugins.BuildPlugins.application)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.kotlinKapt)
  id(Plugins.BuildPlugins.navSafeArgs)
  id(Plugins.BuildPlugins.googleServices)
}

android {
  namespace = "com.google.fhir.examples.cloudfunction"
  compileSdk = Sdk.compileSdk
  defaultConfig {
    applicationId = Releases.CloudFunction.applicationId
    minSdk = Sdk.minSdk
    targetSdk = Sdk.targetSdk
    versionCode = Releases.CloudFunction.versionCode
    versionName = Releases.CloudFunction.versionName
    testInstrumentationRunner = Dependencies.androidJunitRunner
  }
  buildTypes {
    getByName("release") {
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
  coreLibraryDesugaring(Dependencies.desugarJdkLibs)

  implementation(Dependencies.Androidx.activity)
  implementation(Dependencies.Androidx.appCompat)
  implementation(Dependencies.Androidx.constraintLayout)
  implementation(Dependencies.Androidx.datastorePref)
  implementation(Dependencies.Androidx.fragmentKtx)
  implementation(Dependencies.Androidx.recyclerView)
  implementation(Dependencies.Androidx.workRuntimeKtx)
  implementation(platform(Dependencies.Firebase.firebaseBom))
  implementation(Dependencies.Firebase.firebaseUI)
  implementation(Dependencies.Kotlin.kotlinCoroutinesAndroid)
  implementation(Dependencies.Kotlin.kotlinCoroutinesCore)
  implementation(Dependencies.Kotlin.stdlib)
  implementation(Dependencies.Lifecycle.liveDataKtx)
  implementation(Dependencies.Lifecycle.runtime)
  implementation(Dependencies.Lifecycle.viewModelKtx)
  implementation(Dependencies.Navigation.navFragmentKtx)
  implementation(Dependencies.Navigation.navUiKtx)
  implementation(Dependencies.material)
  implementation(Dependencies.timber)
  implementation("com.google.android.fhir:engine:0.1.0-beta03")
  implementation("com.google.android.fhir:data-capture:1.0.0")
}
