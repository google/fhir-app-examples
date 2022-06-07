/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

object Dependencies {

  object Androidx {
    const val activity = "androidx.activity:activity:${Versions.Androidx.activity}"
    const val appCompat = "androidx.appcompat:appcompat:${Versions.Androidx.appCompat}"
    const val constraintLayout =
      "androidx.constraintlayout:constraintlayout:${Versions.Androidx.constraintLayout}"
    const val datastorePref =
      "androidx.datastore:datastore-preferences:${Versions.Androidx.datastorePref}"
    const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.Androidx.fragmentKtx}"
    const val recyclerView = "androidx.recyclerview:recyclerview:${Versions.Androidx.recyclerView}"
    const val workRuntimeKtx = "androidx.work:work-runtime-ktx:${Versions.Androidx.workRuntimeKtx}"
  }

  object Kotlin {
    const val kotlinCoroutinesAndroid =
      "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.Kotlin.kotlinCoroutinesCore}"
    const val kotlinCoroutinesCore =
      "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.kotlinCoroutinesCore}"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.Kotlin.stdlib}"
  }

  object Lifecycle {
    const val liveDataKtx =
      "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.Androidx.lifecycle}"
    const val runtime = "androidx.lifecycle:lifecycle-runtime:${Versions.Androidx.lifecycle}"
    const val viewModelKtx =
      "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.Androidx.lifecycle}"
  }

  object Navigation {
    const val navFragmentKtx =
      "androidx.navigation:navigation-fragment-ktx:${Versions.Androidx.navigation}"
    const val navUiKtx = "androidx.navigation:navigation-ui-ktx:${Versions.Androidx.navigation}"
  }

  object Retrofit {
    const val gsonConverter = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
  }

  const val appAuth = "net.openid:appauth:${Versions.appAuth}"
  const val jwtDecode = "com.auth0.android:jwtdecode:${Versions.jwtDecode}"
  const val desugarJdkLibs = "com.android.tools:desugar_jdk_libs:${Versions.desugarJdkLibs}"
  const val http = "com.squareup.okhttp3:okhttp:${Versions.http}"
  const val kotlinPoet = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"
  const val material = "com.google.android.material:material:${Versions.material}"
  const val timber = "com.jakewharton.timber:timber:${Versions.timber}"

  const val androidJunitRunner = "androidx.test.runner.AndroidJUnitRunner"

  object Versions {
    object Androidx {
      const val activity = "1.2.1"
      const val appCompat = "1.1.0"
      const val constraintLayout = "2.1.1"
      const val datastorePref = "1.0.0"
      const val fragmentKtx = "1.3.1"
      const val lifecycle = "2.2.0"
      const val navigation = "2.3.4"
      const val recyclerView = "1.1.0"
      const val workRuntimeKtx = "2.7.1"
    }

    object Kotlin {
      const val kotlinCoroutinesCore = "1.4.2"
      const val stdlib = "1.6.10"
    }

    const val appAuth = "0.11.1"
    const val desugarJdkLibs = "1.1.5"
    const val http = "4.9.1"
    const val jwtDecode = "2.0.1"
    const val kotlinPoet = "1.9.0"
    const val material = "1.6.0"
    const val retrofit = "2.7.2"
    const val timber = "5.0.1"
  }
}
