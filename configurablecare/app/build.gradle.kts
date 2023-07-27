plugins {
  id("com.android.application")
  id("kotlin-android")
  id("androidx.navigation.safeargs.kotlin")
}

android {
  namespace = "com.google.fhir.examples.configurablecare"
  compileSdk = 33
  defaultConfig {
    applicationId = "com.google.fhir.examples.configurablecare"
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
  packaging { resources.excludes.addAll(listOf(
    "META-INF/ASL-2.0.txt",
    "META-INF/LGPL-3.0.txt",
    "META-INF/LICENSE.md",
    "META-INF/NOTICE.md",
    "META-INF/sun-jaxb.episode",
    "META-INF/DEPENDENCIES"
  ))
  }
}

configurations.all {
  resolutionStrategy {
    force(HapiFhir.caffeine)

    force(HapiFhir.fhirBase)
    force(HapiFhir.fhirClient)
    force(HapiFhir.fhirCoreConvertors)

    force(HapiFhir.fhirCoreDstu2)
    force(HapiFhir.fhirCoreDstu2016)
    force(HapiFhir.fhirCoreDstu3)
    force(HapiFhir.fhirCoreR4)
    force(HapiFhir.fhirCoreR4b)
    force(HapiFhir.fhirCoreR5)
    force(HapiFhir.fhirCoreUtils)

    force(HapiFhir.structuresDstu2)
    force(HapiFhir.structuresDstu3)
    force(HapiFhir.structuresR4)
    force(HapiFhir.structuresR5)

    force(HapiFhir.validation)
    force(HapiFhir.validationDstu3)
    force(HapiFhir.validationR4)
    force(HapiFhir.validationR5)
  }
}

object Versions {
  object Androidx {
    const val activity = "1.7.2"
    const val appCompat = "1.6.1"
    const val constraintLayout = "2.1.4"
    const val coreKtx = "1.10.1"
    const val datastorePref = "1.0.0"
    const val fragmentKtx = "1.6.0"
    const val lifecycle = "2.6.1"
    const val navigation = "2.6.0"
    const val recyclerView = "1.3.0"
    const val room = "2.5.2"
    const val sqliteKtx = "2.3.1"
    const val workRuntimeKtx = "2.8.1"
  }

  object Cql {
    const val engine = "2.4.0"
    const val evaluator = "2.4.0"
    const val translator = "2.4.0"
  }

  object Glide {
    const val glide = "4.14.2"
  }

  object Kotlin {
    const val kotlinCoroutinesCore = "1.7.2"
    const val stdlib = "1.8.20"
  }

  const val androidFhirCommon = "0.1.0-alpha04"
  const val androidFhirEngine = "0.1.0-beta03"
  const val androidFhirKnowledge = "0.1.0-alpha01"
  const val desugarJdkLibs = "2.0.3"
  const val caffeine = "2.9.1"
  const val fhirUcum = "1.0.3"
  const val gson = "2.9.1"
  const val guava = "28.2-android"

  // Hapi FHIR and HL7 Core Components are interlinked.
  // Newer versions of HapiFhir don't work on Android due to the use of Caffeine 3+
  // Wait for this to release (6.3): https://github.com/hapifhir/hapi-fhir/pull/4196
  const val hapiFhir = "6.0.1"

  // Newer versions don't work on Android due to Apache Commons Codec:
  // Wait for this fix: https://github.com/hapifhir/org.hl7.fhir.core/issues/1046
  const val hapiFhirCore = "5.6.36"

  const val http = "4.11.0"
  // Maximum version that supports Android API Level 24:
  // https://github.com/FasterXML/jackson-databind/issues/3658
  const val jackson = "2.13.5"
  const val jsonToolsPatch = "1.13"
  const val jsonAssert = "1.5.1"
  const val material = "1.9.0"
  const val retrofit = "2.9.0"
  const val sqlcipher = "4.5.4"
  const val timber = "5.0.1"
  const val truth = "1.1.5"
  const val woodstox = "6.5.1"
  const val xerces = "2.12.2"
  const val xmlUnit = "2.9.1"

  // Test dependencies
  object AndroidxTest {
    const val benchmarkJUnit = "1.1.1"
    const val core = "1.5.0"
    const val archCore = "2.2.0"
    const val extJunit = "1.1.5"
    const val rules = "1.5.0"
    const val runner = "1.5.0"
    const val fragmentVersion = "1.6.0"
  }

  const val espresso = "3.5.1"
  const val jacoco = "0.8.10"
  const val junit = "4.13.2"
  const val mockitoKotlin = "3.2.0"
  const val mockitoInline = "4.0.0"
  const val robolectric = "4.10.3"

  object Mlkit {
    const val barcodeScanning = "16.1.1"
    const val objectDetection = "16.2.3"
    const val objectDetectionCustom = "16.3.1"
  }
}

object HapiFhir {
  const val fhirBase = "ca.uhn.hapi.fhir:hapi-fhir-base:${Versions.hapiFhir}"
  const val fhirClient = "ca.uhn.hapi.fhir:hapi-fhir-client:${Versions.hapiFhir}"
  const val structuresDstu2 = "ca.uhn.hapi.fhir:hapi-fhir-structures-dstu2:${Versions.hapiFhir}"
  const val structuresDstu3 = "ca.uhn.hapi.fhir:hapi-fhir-structures-dstu3:${Versions.hapiFhir}"
  const val structuresR4 = "ca.uhn.hapi.fhir:hapi-fhir-structures-r4:${Versions.hapiFhir}"
  const val structuresR4b = "ca.uhn.hapi.fhir:hapi-fhir-structures-r4b:${Versions.hapiFhir}"
  const val structuresR5 = "ca.uhn.hapi.fhir:hapi-fhir-structures-r5:${Versions.hapiFhir}"

  const val validation = "ca.uhn.hapi.fhir:hapi-fhir-validation:${Versions.hapiFhir}"
  const val validationDstu3 =
    "ca.uhn.hapi.fhir:hapi-fhir-validation-resources-dstu3:${Versions.hapiFhir}"
  const val validationR4 =
    "ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:${Versions.hapiFhir}"
  const val validationR5 =
    "ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r5:${Versions.hapiFhir}"

  const val fhirCoreDstu2 = "ca.uhn.hapi.fhir:org.hl7.fhir.dstu2:${Versions.hapiFhirCore}"
  const val fhirCoreDstu2016 =
    "ca.uhn.hapi.fhir:org.hl7.fhir.dstu2016may:${Versions.hapiFhirCore}"
  const val fhirCoreDstu3 = "ca.uhn.hapi.fhir:org.hl7.fhir.dstu3:${Versions.hapiFhirCore}"
  const val fhirCoreR4 = "ca.uhn.hapi.fhir:org.hl7.fhir.r4:${Versions.hapiFhirCore}"
  const val fhirCoreR4b = "ca.uhn.hapi.fhir:org.hl7.fhir.r4b:${Versions.hapiFhirCore}"
  const val fhirCoreR5 = "ca.uhn.hapi.fhir:org.hl7.fhir.r5:${Versions.hapiFhirCore}"
  const val fhirCoreUtils = "ca.uhn.hapi.fhir:org.hl7.fhir.utilities:${Versions.hapiFhirCore}"
  const val fhirCoreConvertors =
    "ca.uhn.hapi.fhir:org.hl7.fhir.convertors:${Versions.hapiFhirCore}"

  // Runtime dependency that is required to run FhirPath (also requires minSDK of 26).
  // Version 3.0 uses java.lang.System.Logger, which is not available on Android
  // Replace for Guava when this PR gets merged: https://github.com/hapifhir/hapi-fhir/pull/3977
  const val caffeine = "com.github.ben-manes.caffeine:caffeine:${Versions.caffeine}"
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
  implementation("com.google.android.fhir:engine:0.1.0-beta03")
  implementation("com.google.android.fhir:data-capture:1.0.0")
  implementation("com.google.android.fhir:knowledge:0.1.0-alpha01")
  implementation("com.google.android.fhir:workflow:0.1.0-alpha03-minApi24")
}