plugins {
  id(Plugins.BuildPlugins.application)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.kotlinKapt)
  id(Plugins.BuildPlugins.navSafeArgs)
}

android {
  namespace = "com.google.fhir.examples.demo"
  compileSdk = Sdk.compileSdk
  defaultConfig {
    applicationId = Releases.Demo.applicationId
    minSdk = Sdk.minSdk
    targetSdk = Sdk.targetSdk
    versionCode = Releases.Demo.versionCode
    versionName = Releases.Demo.versionName
    testInstrumentationRunner = Dependencies.androidJunitRunner
    manifestPlaceholders["appAuthRedirectScheme"] = applicationId!!
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
  implementation(Dependencies.Androidx.fragmentKtx)
  implementation(Dependencies.Androidx.recyclerView)
  implementation(Dependencies.Androidx.workRuntimeKtx)
  implementation(Dependencies.Kotlin.kotlinCoroutinesAndroid)
  implementation(Dependencies.Kotlin.kotlinCoroutinesCore)
  implementation(Dependencies.Kotlin.stdlib)
  implementation(Dependencies.Lifecycle.liveDataKtx)
  implementation(Dependencies.Lifecycle.runtime)
  implementation(Dependencies.Lifecycle.viewModelKtx)
  implementation(Dependencies.Navigation.navFragmentKtx)
  implementation(Dependencies.Navigation.navUiKtx)
  implementation(Dependencies.Retrofit.gsonConverter)
  implementation(Dependencies.Androidx.datastorePref)
  implementation(Dependencies.material)
  implementation(Dependencies.timber)
  implementation(Dependencies.appAuth)
  implementation(Dependencies.jwtDecode)
  implementation("com.google.android.fhir:engine:0.1.0-beta03")
  implementation("com.google.android.fhir:data-capture:1.0.0")
}
