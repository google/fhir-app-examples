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
package com.google.fhir.examples.configurablecare.screening

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.fhir.examples.configurablecare.FhirApplication
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.MedicationRequest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Task

class TaskViewPagerViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {
  val livePendingActivitiesCount = MutableLiveData<Int>()
  val liveCompletedActivitiesCount = MutableLiveData<Int>()
  val patientName = MutableLiveData<String>()

  private val requestManager =
    FhirApplication.requestManager(getApplication<Application>().applicationContext)
  private val fhirEngine =
    FhirApplication.fhirEngine(getApplication<Application>().applicationContext)

  fun getTasksCount(patientId: String) {
    viewModelScope.launch {
      livePendingActivitiesCount.value =
        requestManager.getRequestsCount(patientId, status = "draft") +
          requestManager.getRequestsCount(patientId, status = "active") +
          requestManager.getRequestsCount(patientId, status = "on-hold")

      val liveCompletedTasks =
        requestManager.getAllRequestsForPatient(patientId, "completed") +
          requestManager.getAllRequestsForPatient(patientId, "cancelled") +
          requestManager.getAllRequestsForPatient(patientId, "stopped")

      val orders: MutableList<Resource> = mutableListOf()
      val plans: MutableList<Resource> = mutableListOf()
      val proposals: MutableList<Resource> = mutableListOf()
      val miscRequests: MutableList<Resource> = mutableListOf()
      for (request in liveCompletedTasks) {
        if (request is ServiceRequest || request is Task) {
          miscRequests.add(request)
        } else if (request is MedicationRequest) {
          if (request.intent == MedicationRequest.MedicationRequestIntent.ORDER) {
            orders.add(request)
          }
        }
      }
      for (request in liveCompletedTasks) {
        if (request is MedicationRequest) {
          if (request.intent == MedicationRequest.MedicationRequestIntent.PLAN &&
              (request.status != MedicationRequest.MedicationRequestStatus.COMPLETED ||
                orders.size == 0)
          ) {
            plans.add(request)
          }
        }
      }
      for (request in liveCompletedTasks) {
        if (request is MedicationRequest) {
          if (request.intent == MedicationRequest.MedicationRequestIntent.PROPOSAL &&
              (request.status != MedicationRequest.MedicationRequestStatus.COMPLETED ||
                (orders.size == 0 && plans.size == 0))
          ) {
            proposals.add(request)
          }
        }
      }
      val requests = orders + proposals + plans + miscRequests
      liveCompletedActivitiesCount.value = requests.size
    }
  }

  fun getPatientName(patientId: String) {
    viewModelScope.launch {
      patientName.value =
        (fhirEngine.get(ResourceType.Patient, patientId) as Patient).name[0].nameAsSingleString
    }
  }

  fun getActivityStatus(position: Int): String {
    return when (position) {
      0 -> "draft"
      1 -> "completed"
      else -> "ready"
    }
  }
}
