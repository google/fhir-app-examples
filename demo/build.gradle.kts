// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:8.0.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    classpath("com.google.gms:google-services:4.3.15")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:6.6.0")
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0")

    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle.kts files
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  configureSpotless()
}

fun Project.configureSpotless() {
  val ktlintVersion = "0.41.0"
  val ktlintOptions = mapOf("indent_size" to "2", "continuation_indent_size" to "2")
  apply(plugin = "com.diffplug.spotless")
  configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    ratchetFrom = "origin/main"
    kotlin {
      target("**/*.kt")
      targetExclude("**/build/")
      targetExclude("**/*_Generated.kt")
      ktlint(ktlintVersion).userData(ktlintOptions)
      ktfmt().googleStyle()
      licenseHeaderFile(
        "${project.rootProject.projectDir}/license-header.txt",
        "package|import|class|object|sealed|open|interface|abstract "
        // It is necessary to tell spotless the top level of a file in order to apply config to it
        // See: https://github.com/diffplug/spotless/issues/135
        )
    }
    kotlinGradle {
      target("*.gradle.kts")
      ktlint(ktlintVersion).userData(ktlintOptions)
      ktfmt().googleStyle()
    }
    format("xml") {
      target("**/*.xml")
      targetExclude("**/build/", ".idea/")
      prettier(mapOf("prettier" to "2.0.5", "@prettier/plugin-xml" to "0.13.0"))
        .config(mapOf("parser" to "xml", "tabWidth" to 4))
    }
    // Creates one off SpotlessApply task for generated files
    com.diffplug.gradle.spotless.KotlinExtension(this).apply {
      target("**/*_Generated.kt")
      ktlint(ktlintVersion).userData(ktlintOptions)
      ktfmt().googleStyle()
      licenseHeaderFile(
        "${project.rootProject.projectDir}/license-header.txt",
        "package|import|class|object|sealed|open|interface|abstract "
      )
      createIndependentApplyTask("spotlessGenerated")
    }
  }
}
