/*
 * Copyright 2022-2023 Google LLC
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
package com.google.fhir.examples.demo.data

import android.content.Context
import androidx.work.WorkerParameters
import com.auth0.android.jwt.JWT
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.fhir.examples.demo.FhirApplication
import com.google.fhir.examples.demo.LoginRepository

class FhirSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {

  override fun getDownloadWorkManager(): DownloadWorkManager {
    val loginRepository = LoginRepository.getInstance(applicationContext)
    val accessToken = loginRepository.getAccessToken()
    val patientListId = JWT(accessToken).getClaim("patient_list").asString()
    return DownloadWorkManagerImpl(patientListId!!)
  }

  override fun getConflictResolver() = AcceptLocalConflictResolver

  override fun getFhirEngine() = FhirApplication.fhirEngine(applicationContext)
}
