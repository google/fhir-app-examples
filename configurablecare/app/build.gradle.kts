import Build_gradle.Dependencies.forceGuava
import Build_gradle.Dependencies.forceHapiVersion
import Build_gradle.Dependencies.forceJacksonVersion
import Build_gradle.Dependencies.removeIncompatibleDependencies

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
  packaging {
    resources.excludes.addAll(
      listOf(
        "META-INF/ASL-2.0.txt",
        "META-INF/LGPL-3.0.txt",
        "META-INF/LICENSE.md",
        "META-INF/NOTICE.md",
        "META-INF/sun-jaxb.episode",
        "META-INF/DEPENDENCIES"
      )
    )
  }
}

configurations {
  all {
    removeIncompatibleDependencies()
    forceGuava()
    forceHapiVersion()
    forceJacksonVersion()
  }
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
  implementation("com.google.android.fhir:engine:0.1.0-beta05")
  implementation("com.google.android.fhir:data-capture:1.0.0")
  implementation("com.google.android.fhir:knowledge:0.1.0-alpha03")
  implementation("com.google.android.fhir:workflow:0.1.0-alpha04")
}

/**
 * workflow library strictly depends on the versions of the dependencies defined here.
 * [Configuration.force**] functions defined here force gradle to pick correct versions of the
 * dependencies as required by wprkflow.
 */
object Dependencies {
  const val guava = "com.google.guava:guava:${Versions.guava}"
  object HapiFhir {
    const val fhirBase = "ca.uhn.hapi.fhir:hapi-fhir-base:${Versions.hapiFhir}"
    const val fhirClient = "ca.uhn.hapi.fhir:hapi-fhir-client:${Versions.hapiFhir}"
    const val structuresDstu2 = "ca.uhn.hapi.fhir:hapi-fhir-structures-dstu2:${Versions.hapiFhir}"
    const val structuresDstu3 = "ca.uhn.hapi.fhir:hapi-fhir-structures-dstu3:${Versions.hapiFhir}"
    const val structuresR4 = "ca.uhn.hapi.fhir:hapi-fhir-structures-r4:${Versions.hapiFhir}"
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

    const val guavaCaching = "ca.uhn.hapi.fhir:hapi-fhir-caching-guava:${Versions.hapiFhir}"
  }

  object Jackson {
    private const val mainGroup = "com.fasterxml.jackson"
    private const val coreGroup = "$mainGroup.core"
    private const val dataformatGroup = "$mainGroup.dataformat"
    private const val datatypeGroup = "$mainGroup.datatype"
    private const val moduleGroup = "$mainGroup.module"

    const val annotations = "$coreGroup:jackson-annotations:${Versions.jackson}"
    const val bom = "$mainGroup:jackson-bom:${Versions.jackson}"
    const val core = "$coreGroup:jackson-core:${Versions.jacksonCore}"
    const val databind = "$coreGroup:jackson-databind:${Versions.jackson}"
    const val dataformatXml = "$dataformatGroup:jackson-dataformat-xml:${Versions.jackson}"
    const val jaxbAnnotations = "$moduleGroup:jackson-module-jaxb-annotations:${Versions.jackson}"
    const val jsr310 = "$datatypeGroup:jackson-datatype-jsr310:${Versions.jackson}"
  }
  object Versions {
    object Cql {
      const val clinicalReasoning = "3.0.0-PRE9-SNAPSHOT"
    }
    const val guava = "32.1.2-android"
    const val hapiFhir = "6.8.0"
    const val hapiFhirCore = "6.0.22"
    // Maximum Jackson libraries (excluding core) version that supports Android API Level 24:
    // https://github.com/FasterXML/jackson-databind/issues/3658
    const val jackson = "2.13.5"
    // Maximum Jackson Core library version that supports Android API Level 24:
    const val jacksonCore = "2.15.2"
  }

  fun Configuration.removeIncompatibleDependencies() {
    exclude(module = "xpp3")
    exclude(module = "xpp3_min")
    exclude(module = "xmlpull")
    exclude(module = "javax.json")
    exclude(module = "jcl-over-slf4j")
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "org.antlr", module = "antlr4")
    exclude(group = "org.eclipse.persistence", module = "org.eclipse.persistence.moxy")
  }

  fun Configuration.forceGuava() {
    // Removes caffeine
    exclude(module = "hapi-fhir-caching-caffeine")
    exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")

    resolutionStrategy {
      force(guava)
      force(HapiFhir.guavaCaching)
    }
  }

  fun Configuration.forceHapiVersion() {
    // Removes newer versions of caffeine and manually imports 2.9
    // Removes newer versions of hapi and keeps on 6.0.1
    // (newer versions don't work on Android)
    resolutionStrategy {
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

  fun Configuration.forceJacksonVersion() {
    resolutionStrategy {
      force(Jackson.annotations)
      force(Jackson.bom)
      force(Jackson.core)
      force(Jackson.databind)
      force(Jackson.jaxbAnnotations)
      force(Jackson.jsr310)
      force(Jackson.dataformatXml)
    }
  }
}
